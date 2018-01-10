package io.rhizomatic.kernel;

import io.rhizomatic.api.Monitor;
import io.rhizomatic.api.SystemDefinition;
import io.rhizomatic.api.layer.RzLayer;
import io.rhizomatic.api.layer.RzModule;
import io.rhizomatic.api.web.WebApp;
import io.rhizomatic.kernel.parse.ArgsParser;
import io.rhizomatic.kernel.system.RhizomaticSystem;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;

import static io.rhizomatic.kernel.monitor.MonitorSetup.initializeMonitor;
import static io.rhizomatic.kernel.monitor.MonitorSetup.redirectJdkLogging;
import static io.rhizomatic.kernel.spi.ConfigurationKeys.DOMAIN;
import static io.rhizomatic.kernel.spi.ConfigurationKeys.ENVIRONMENT;
import static io.rhizomatic.kernel.spi.ConfigurationKeys.RUNTIME;

/**
 * Main entry for a Rhizomatic system.
 */
public class Rhizomatic {
    private volatile RhizomaticState state = RhizomaticState.UNINITIALIZED;

    private Path root;
    private RhizomaticSystem system;
    private Monitor monitor;
    private boolean moduleMode = true;
    private boolean silent;

    private List<RzLayer> layers = Collections.emptyList();
    private List<WebApp> webApps = Collections.emptyList();

    private Set<Class<?>> services = Collections.emptySet();
    private Set<Object> instances = Collections.emptySet();

    private Map<String, String> configuration = new HashMap<>();

    /**
     * Launches a system.
     */
    public static void main(String... args) {
        long start = System.currentTimeMillis();

        Optional<SystemDefinition> definition = ServiceLoader.load(SystemDefinition.class).findFirst();

        Map<String, String> base = definition.isPresent() ? definition.get().getConfiguration() : new HashMap<>();
        Map<String, String> configuration = loadConfiguration(base);

        Monitor monitor = initializeMonitor(configuration);

        Builder builder = Builder.newInstance();

        builder.configuration(configuration);
        builder.monitor(monitor);

        ArgsParser.Params params = ArgsParser.parseParams(args);

        List<RzLayer> rzLayers;
        if (params.getModulePath() != null) {
            RzModule rzModule = new RzModule(Paths.get(params.getModulePath()));
            rzLayers = List.of(RzLayer.Builder.newInstance("main").module(rzModule).build());
        } else if (definition.isPresent()) {
            rzLayers = definition.get().getLayers();
        } else {
            throw new IllegalArgumentException("Either a library that provides " + SystemDefinition.class.getName() + " must be present or a module path must be specified");
        }

        builder.layers(rzLayers);

        if (definition.isPresent()) {
            List<WebApp> webApps = definition.get().getWebApps();
            if (webApps == null) {
                throw new IllegalArgumentException("Web apps cannot be null. If there are no configured web apps, return an empty collection from the definition.");
            }
            builder.webApps(webApps);
        }

        Rhizomatic rhizomatic = builder.build();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            rhizomatic.shutdown();
            monitor.info(() -> "Shutdown complete");
        }));

        monitor.info(() -> "Starting " + configuration.get(DOMAIN) + "/" + configuration.get(RUNTIME));

        rhizomatic.start();

        rhizomatic.monitor.info(() -> "Boot time: " + (System.currentTimeMillis() - start) + "ms");
        rhizomatic.monitor.info(() -> "Ready [" + rhizomatic.configuration.get(ENVIRONMENT) + "]");
    }

    /**
     * Returns the current system state.
     */
    public RhizomaticState getState() {
        return state;
    }

    /**
     * Transitions the system to be ready to receive requests.
     */
    public void start() {
        if (configuration == null) {
            configuration = loadConfiguration(Collections.emptyMap());
        }
        if (silent) {
            // create a null monitor
            monitor = new Monitor() {
            };
            redirectJdkLogging(monitor);
        } else if (monitor == null) {
            monitor = initializeMonitor(configuration);
        }
        system = new RhizomaticSystem(monitor, configuration);

        system.startSubsystems();

        if (moduleMode) {
            system.instantiateLayers(layers);
        } else {
            system.instantiateClasspath(services, instances);
        }

        system.defineWebApps(webApps);

        state = RhizomaticState.INITIALIZED;

        system.start();

        state = RhizomaticState.STARTED;
    }

    /**
     * Shuts the system down.
     */
    public void shutdown() {
        if (RhizomaticState.STARTED != state) {
            return;
        }
        system.shutdown();
        state = RhizomaticState.SHUTDOWN;
    }

    private static Map<String, String> loadConfiguration(Map<String, String> base) {
        Map<String, String> configuration = new HashMap<>(base);
        configuration.putIfAbsent(DOMAIN, "domain");
        configuration.putIfAbsent(RUNTIME, "runtime");
        configuration.putIfAbsent(ENVIRONMENT, "production");
        return configuration;
    }

    public static class Builder {
        private Rhizomatic rhizomatic;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder configuration(Map<String, String> configuration) {
            rhizomatic.configuration = loadConfiguration(configuration);
            return this;
        }

        public Builder monitor(Monitor monitor) {
            rhizomatic.monitor = monitor;
            return this;
        }

        public Builder environment(String environment) {
            return this;
        }

        public Builder moduleMode(boolean value) {
            rhizomatic.moduleMode = value;
            return this;
        }

        public Builder silent(boolean value) {
            rhizomatic.silent = value;
            return this;
        }

        public Builder services(Class<?>... classes) {
            this.services(Set.of(classes));
            return this;
        }

        public Builder services(Set<Class<?>> classes) {
            Objects.requireNonNull(classes, "Services cannot be null");
            rhizomatic.services = classes;
            return this;
        }

        public Builder instances(Object... instances) {
            this.instances(Set.of(instances));
            return this;
        }

        public Builder instances(Set<Object> instances) {
            Objects.requireNonNull(instances, "Service instances cannot be null");
            rhizomatic.instances = instances;
            return this;
        }

        public Builder layers(List<RzLayer> layers) {
            rhizomatic.layers = layers;
            return this;
        }

        public Builder webApps(List<WebApp> webApps) {
            Set<String> contextPaths = new HashSet<>();
            for (WebApp webApp : webApps) {
                String contextPath = webApp.getContextPath();
                if (contextPaths.contains(contextPath)) {
                    throw new IllegalArgumentException("More than one web app configured for the context path: " + contextPath);
                }
                contextPaths.add(contextPath);
            }
            rhizomatic.webApps = webApps;
            return this;
        }

        public Builder root(Path root) {
            rhizomatic.root = root;
            return this;
        }

        public Rhizomatic build() {
            return rhizomatic;
        }

        private Builder() {
            this.rhizomatic = new Rhizomatic();
        }

    }


}
