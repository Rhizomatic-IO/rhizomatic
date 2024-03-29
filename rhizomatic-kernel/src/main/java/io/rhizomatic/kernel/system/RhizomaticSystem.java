package io.rhizomatic.kernel.system;

import io.rhizomatic.api.Monitor;
import io.rhizomatic.api.RhizomaticException;
import io.rhizomatic.api.ServiceContext;
import io.rhizomatic.api.layer.RzLayer;
import io.rhizomatic.api.web.WebApp;
import io.rhizomatic.kernel.layer.LayerManager;
import io.rhizomatic.kernel.layer.LayerSubsystem;
import io.rhizomatic.kernel.reload.ReloaderSubsystem;
import io.rhizomatic.kernel.scan.ClassScanner;
import io.rhizomatic.kernel.scan.ScannerSubsystem;
import io.rhizomatic.kernel.spi.inject.InstanceManager;
import io.rhizomatic.kernel.spi.layer.LoadedLayer;
import io.rhizomatic.kernel.spi.scan.ScanIndex;
import io.rhizomatic.kernel.spi.subsystem.Subsystem;
import io.rhizomatic.kernel.spi.subsystem.SubsystemContext;
import io.rhizomatic.kernel.spi.subsystem.Subsystems;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import static io.rhizomatic.kernel.spi.ConfigurationKeys.ENVIRONMENT;
import static io.rhizomatic.kernel.spi.ConfigurationKeys.RUNTIME;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

/**
 * Manages a Rhizomatic system.
 */
public class RhizomaticSystem implements SubsystemContext {

    private List<Subsystem> subsystems;

    private RzServiceContext serviceContext;

    private Map<Class<?>, List<Object>> systemServices = new HashMap<>();

    private List<LoadedLayer> loadedLayers = Collections.emptyList();

    private List<WebApp> webApps = Collections.emptyList();

    private Map<String, Object> configuration;

    private Monitor monitor;

    private boolean subsystemsStarted;

    public RhizomaticSystem(Monitor monitor, Map<String, Object> configuration) {
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
        var layerManager = resolve(LayerManager.class);

        var openToModules = subsystems.stream().flatMap(s -> s.openModulesTo().stream()).collect(toSet());

        loadedLayers = layerManager.load(layers, openToModules);

        var scanIndex = scan(loadedLayers);

        var instanceManager = resolve(InstanceManager.class);

        instanceManager.register(Monitor.class, monitor);

        //TODO populate context from configuration
        serviceContext = new RzServiceContext((String) configuration.get(RUNTIME), (String) configuration.get(ENVIRONMENT));
        instanceManager.register(ServiceContext.class, serviceContext);

        instanceManager.wire(scanIndex);
    }

    public void instantiateClasspath(Set<Class<?>> classes, Set<Object> instances) {
        var classScanner = resolve(ClassScanner.class);

        var scanIndex = classScanner.scan(classes);

        var instanceManager = resolve(InstanceManager.class);

        instanceManager.register(Monitor.class, monitor);
        instances.forEach(instance -> {
            var clazz = instance.getClass();
            if (clazz.getInterfaces().length == 1 && !clazz.getPackageName().startsWith("java.")) {
                instanceManager.register(clazz.getInterfaces()[0], instance);
            }
            instanceManager.register(clazz, instance);
        });

        //TODO populate context from configuration
        var serviceContext = new RzServiceContext((String) configuration.get(RUNTIME), (String) configuration.get(ENVIRONMENT));
        instanceManager.register(ServiceContext.class, serviceContext);

        instanceManager.wire(scanIndex);
    }

    public void defineWebApps(List<WebApp> webApps) {
        this.webApps = webApps; // web apps are handled during application initialization when the system is started
    }

    public void start() {
        subsystems.forEach(subsystem -> subsystem.applicationInitialize(this));
        subsystems.forEach(subsystem -> subsystem.start(this));
        serviceContext.bootComplete();
    }

    public void shutdown() {
        var iterator = subsystems.listIterator(subsystems.size());
        while (iterator.hasPrevious()) {
            iterator.previous().shutdown();
        }
        serviceContext.shutdownComplete();
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
        requireNonNull(service, "Service was null");
        var list = systemServices.computeIfAbsent(type, k -> new ArrayList<>());
        list.add(service);
    }

    public <T> T resolve(Class<T> type) {
        return resolveAll(type).get(0);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> resolveAll(Class<T> type) {
        var list = (List<T>) systemServices.get(type);
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
        var classScanner = resolve(ClassScanner.class);
        return classScanner.scan(loadedLayers);
    }

    /**
     * Composes the kernel and extension subsystems.
     */
    private List<Subsystem> composeSubsystems() {
        var subsystems = new ArrayList<>(Subsystems.getInstalled());

        var scannerSubsystem = new ScannerSubsystem();
        subsystems.add(scannerSubsystem);

        var layerSubsystem = new LayerSubsystem();
        subsystems.add(layerSubsystem);

        var reloaderSubsystem = new ReloaderSubsystem();
        subsystems.add(reloaderSubsystem);

        var extensions = ServiceLoader.load(Subsystem.class);
        for (var extension : extensions) {
            if (subsystems.contains(extension)) {
                continue; // subsystem is manually installed, e.g. from a test fixture; skip the service loader instance
            }
            subsystems.add(extension);
        }
        return subsystems;
    }

}
