package io.rhizomatic.kernel.system;

import io.rhizomatic.api.Monitor;
import io.rhizomatic.api.RhizomaticException;
import io.rhizomatic.api.ServiceContext;
import io.rhizomatic.api.layer.RzLayer;
import io.rhizomatic.api.web.WebApp;
import io.rhizomatic.kernel.layer.LayerManager;
import io.rhizomatic.kernel.layer.LayerSubsystem;
import io.rhizomatic.kernel.spi.layer.LoadedLayer;
import io.rhizomatic.kernel.scan.ClassScanner;
import io.rhizomatic.kernel.scan.ScannerSubsystem;
import io.rhizomatic.kernel.spi.inject.InstanceManager;
import io.rhizomatic.kernel.spi.scan.ScanIndex;
import io.rhizomatic.kernel.spi.subsystem.Subsystem;
import io.rhizomatic.kernel.spi.subsystem.SubsystemContext;
import io.rhizomatic.kernel.spi.subsystem.Subsystems;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.module.ModuleReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;

import static io.rhizomatic.kernel.spi.ConfigurationKeys.DOMAIN;
import static io.rhizomatic.kernel.spi.ConfigurationKeys.ENVIRONMENT;
import static io.rhizomatic.kernel.spi.ConfigurationKeys.RUNTIME;
import static io.rhizomatic.kernel.spi.util.ClassHelper.getClassName;
import static java.util.stream.Collectors.toSet;

/**
 * Manages a Rhizomatic system.
 */
public class RhizomaticSystem implements SubsystemContext {
    private Map<String, String> configuration;

    private List<Subsystem> subsystems;

    private Map<Class<?>, List<Object>> systemServices = new HashMap<>();

    private Monitor monitor;

    private boolean subsystemsStarted;

    private List<LoadedLayer> loadedLayers = Collections.emptyList();

    private List<WebApp> webApps = Collections.emptyList();

    public RhizomaticSystem(Monitor monitor, Map<String, String> configuration) {
        this.monitor = monitor;
        systemServices.put(Monitor.class, List.of(monitor));
        this.configuration = configuration;
    }

    public void startSubsystems() {
        if (subsystemsStarted) {
            throw new IllegalStateException("Subsystems already started");
        }

        // create the kernel and extension subsystems
        subsystems = composeSubsystems();

        subsystems.forEach(subsystem -> subsystem.instantiate(this));
        subsystems.forEach(subsystem -> subsystem.assemble(this));

        subsystemsStarted = true;
    }

    public void instantiateLayers(List<RzLayer> layers) {
        LayerManager layerManager = resolve(LayerManager.class);

        Set<String> openToModules = subsystems.stream().flatMap(s -> s.openModulesTo().stream()).collect(toSet());

        loadedLayers = layerManager.load(layers, openToModules);

        ScanIndex scanIndex = scan(loadedLayers);

        InstanceManager instanceManager = resolve(InstanceManager.class);

        instanceManager.register(Monitor.class, monitor);

        //TODO populate context from configuration
        RzServiceContext serviceContext = new RzServiceContext(configuration.get(RUNTIME), configuration.get(DOMAIN), configuration.get(ENVIRONMENT));
        instanceManager.register(ServiceContext.class, serviceContext);

        instanceManager.wire(scanIndex);
    }

    public void instantiateClasspath(Set<Class<?>> classes, Set<Object> instances) {
        ClassScanner classScanner = resolve(ClassScanner.class);

        classes.forEach(classScanner::addClass);

        ScanIndex scanIndex = classScanner.scan();

        InstanceManager instanceManager = resolve(InstanceManager.class);

        instanceManager.register(Monitor.class, monitor);
        instances.forEach(instance -> {
            Class<?> clazz = instance.getClass();
            if (clazz.getInterfaces().length == 1 && !clazz.getPackageName().startsWith("java.")) {
                instanceManager.register(clazz.getInterfaces()[0], instance);
            }
            instanceManager.register(clazz, instance);
        });

        //TODO populate context from configuration
        RzServiceContext serviceContext = new RzServiceContext(configuration.get(RUNTIME), configuration.get(DOMAIN), configuration.get(ENVIRONMENT));
        instanceManager.register(ServiceContext.class, serviceContext);

        instanceManager.wire(scanIndex);
    }

    public void defineWebApps(List<WebApp> webApps) {
        this.webApps = webApps; // web apps are handled during application initialization when the system is started
    }

    public void start() {
        subsystems.forEach(subsystem -> subsystem.applicationInitialize(this));
        subsystems.forEach(subsystem -> subsystem.start(this));
    }

    public void shutdown() {
        ListIterator<Subsystem> iterator = subsystems.listIterator(subsystems.size());
        while (iterator.hasPrevious()) {
            iterator.previous().shutdown();
        }
    }

    public Monitor getMonitor() {
        return monitor;
    }

    public List<LoadedLayer> getLoadedLayers() {
        return loadedLayers;
    }

    public List<WebApp> getWebApps() {
        return webApps;
    }

    public <T> void registerService(Class<T> type, T service) {
        Objects.requireNonNull(service, "Service was null");
        List<Object> list = systemServices.computeIfAbsent(type, k -> new ArrayList<>());
        list.add(service);
    }

    public <T> T resolve(Class<T> type) {
        return resolveAll(type).get(0);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> resolveAll(Class<T> type) {
        List<T> list = (List<T>) systemServices.get(type);
        if (list == null) {
            throw new RhizomaticException("System service not found for type: " + type.getName() + ". Ensure extensions are properly installed.");
        } else {
            return list;
        }
    }

    public <T> @Nullable T getConfiguration(Class<T> type, String key) {
        return type.cast(configuration.get(key));
    }

    /**
     * Scans all loaded layers for services.
     */
    private ScanIndex scan(List<LoadedLayer> loadedLayers) {
        ClassScanner classScanner = resolve(ClassScanner.class);

        for (LoadedLayer loadedLayer : loadedLayers) {
            for (ModuleReference reference : loadedLayer.getReferences()) {
                try {
                    reference.open().list().forEach(fileName -> {
                        if (fileName.endsWith(".class") && !fileName.startsWith("module-info") && !fileName.startsWith("package-info")) {
                            try {
                                Module module = loadedLayer.getModule(reference);
                                // Important: load using the module classloader so the class is loaded using the module
                                Class<?> type = module.getClassLoader().loadClass(getClassName(fileName));
                                classScanner.addClass(type);
                            } catch (ClassNotFoundException e) {
                                throw new RhizomaticException("Error loading module: " + reference.descriptor().name(), e);
                            }
                        }
                    });
                } catch (IOException e) {
                    throw new RhizomaticException("Error loading module: " + reference.descriptor().name(), e);
                }
            }
        }

        return classScanner.scan();
    }

    /**
     * Composes the kernel and extension subsystems.
     */
    private List<Subsystem> composeSubsystems() {
        List<Subsystem> subsystems = new ArrayList<>(Subsystems.getInstalled());

        Subsystem scannerSubsystem = new ScannerSubsystem();
        subsystems.add(scannerSubsystem);

        LayerSubsystem layerSubsystem = new LayerSubsystem();
        subsystems.add(layerSubsystem);

        Iterable<Subsystem> extensions = ServiceLoader.load(Subsystem.class);
        for (Subsystem extension : extensions) {
            subsystems.add(extension);
        }
        return subsystems;
    }

}
