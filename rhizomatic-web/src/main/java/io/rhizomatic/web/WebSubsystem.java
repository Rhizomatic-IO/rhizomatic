package io.rhizomatic.web;

import io.rhizomatic.api.Monitor;
import io.rhizomatic.api.annotations.EndpointPath;
import io.rhizomatic.api.web.WebApp;
import io.rhizomatic.kernel.spi.inject.InstanceManager;
import io.rhizomatic.kernel.spi.reload.ReloadListener;
import io.rhizomatic.kernel.spi.reload.RzReloader;
import io.rhizomatic.kernel.spi.scan.Introspector;
import io.rhizomatic.kernel.spi.subsystem.Subsystem;
import io.rhizomatic.kernel.spi.subsystem.SubsystemContext;
import io.rhizomatic.web.http.JettyTransport;
import io.rhizomatic.web.jersey.RzInjectionManager;
import io.rhizomatic.web.jersey.RzInjectionManagerFactory;
import io.rhizomatic.web.scan.WebIntrospector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.Source;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.servlet.internal.Utils;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads the Web subsystem. Provides a Jetty web server, web app support, and JAX-RS endpoint publishing using Jersey.
 */
public class WebSubsystem extends Subsystem {
    private static final Set<String> OPENS = Set.of("jersey.common", "jersey.server", "jetty.server", "jetty.util", "io.rhizomatic.web");
    private static final String RHIZOMATIC_REST = "RhizomaticRest";

    protected Monitor monitor;
    protected JettyTransport jettyTransport;

    private Map<Object, Holder> containerMap = new ConcurrentHashMap<>();

    public WebSubsystem() {
        super("rhizomatic.web");
    }

    public Set<String> openModulesTo() {
        return OPENS;
    }

    public void instantiate(SubsystemContext context) {
        monitor = context.getMonitor();
        RzInjectionManagerFactory.INSTANCE = new RzInjectionManager(monitor);

        jettyTransport = new JettyTransport();
        jettyTransport.initialize(context);
        context.registerService(JettyTransport.class, jettyTransport);

        WebIntrospector introspector = new WebIntrospector();
        context.registerService(Introspector.class, introspector);
    }

    public void assemble(SubsystemContext context) {
        context.resolve(RzReloader.class).register(new WebReloadListener());
    }

    public void applicationInitialize(SubsystemContext context) {
        InstanceManager instanceManager = context.resolve(InstanceManager.class);

        // bootstrap Jersey endpoints and providers
        Map<String, ResourceConfig> resourceConfiguration = configureResources(instanceManager);
        configureProviders(resourceConfiguration, instanceManager);
        exportResources(resourceConfiguration);

        // create web app contexts
        createWebApps(context);
    }

    public void start(SubsystemContext context) {
        jettyTransport.start(context);
    }

    public void shutdown() {
        if (jettyTransport != null) {
            jettyTransport.shutdown();
            jettyTransport = null;
        }
        monitor = null;
    }

    protected void configureProviders(Map<String, ResourceConfig> resourceConfiguration, InstanceManager instanceManager) {
        Set<?> providers = instanceManager.resolveQualifiedTypes(Provider.class);
        for (ResourceConfig resourceConfig : resourceConfiguration.values()) {
            for (Object provider : providers) {
                resourceConfig.register(provider);
            }
        }
    }

    protected Map<String, ResourceConfig> configureResources(InstanceManager instanceManager) {
        Set<?> endpoints = instanceManager.resolveQualifiedTypes(Path.class);
        Map<String, ResourceConfig> resourceConfigs = new HashMap<>();
        for (Object endpoint : endpoints) {
            EndpointPath endpointPath = endpoint.getClass().getModule().getAnnotation(EndpointPath.class);
            String rootPath = endpointPath != null ? endpointPath.value() : "api";
            ResourceConfig resourceConfig = resourceConfigs.computeIfAbsent(rootPath, k -> new ResourceConfig());
            resourceConfig.register(endpoint);
        }
        return resourceConfigs;
    }

    protected void exportResources(Map<String, ResourceConfig> configurations) {
        for (Map.Entry<String, ResourceConfig> entry : configurations.entrySet()) {
            String rootPath = entry.getKey();

            ResourceConfig resourceConfig = entry.getValue();

            ServletContainer servletContainer = new ServletContainer();
            for (Object instance : resourceConfig.getSingletons()) {
                containerMap.put(instance, new Holder(rootPath, servletContainer, resourceConfig));
            }

            ServletHolder servletHolder = new ServletHolder(Source.EMBEDDED);
            servletHolder.setName(RHIZOMATIC_REST + "_" + rootPath);
            servletHolder.setServlet(servletContainer);
            servletHolder.setInitOrder(1);

            ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
            handler.setContextPath("/");
            jettyTransport.registerHandler(handler);
            handler.getServletHandler().addServletWithMapping(servletHolder, "/" + rootPath + "/*");

            Utils.store(resourceConfig, handler.getServletContext(), RHIZOMATIC_REST + "_" + rootPath);
            monitor.info(() -> "Endpoint context at: " + (rootPath.startsWith("/") ? rootPath : "/" + rootPath));
        }
    }

    protected void createWebApps(SubsystemContext context) {
        for (WebApp webApp : context.getWebApps()) {
            java.nio.file.Path[] contentRoots = webApp.getContentRoots();

            String[] rootStrings = Arrays.stream(contentRoots).map(java.nio.file.Path::toString).toArray(String[]::new);
            ResourceCollection resources = new ResourceCollection(rootStrings);

            String contextPath = webApp.getContextPath();
            ResourceHandler resourceHandler = new ResourceHandler();
            resourceHandler.setBaseResource(resources);
            ContextHandler ctx = new ContextHandler(contextPath); /* the server uri path */
            ctx.setHandler(resourceHandler);
            jettyTransport.registerHandler(ctx);

            monitor.info(() -> "Web app at: " + (contextPath.startsWith("/") ? contextPath : "/" + contextPath));
        }
    }

    /**
     * Listener to reload resource endpoints.
     */
    private class WebReloadListener implements ReloadListener {

        public void onInstanceChanged(Object instance) {
            Holder holder = containerMap.get(instance);
            if (holder == null) {
                return;
            }
            setContext(holder);

        }

        public void onInstanceAdded(Object instance) {
            Holder holder = containerMap.get(instance);
            if (holder == null) {
                return;
            }
            setContext(holder);
        }

        private void setContext(Holder holder) {
            // The resource context needs to be reset so it is available when the web context is reloaded; Utils.store() removes the context
            // when it is called so it is no longer in the servlet context after the initial load has completed.
            Utils.store(holder.resourceConfig, holder.servletContainer.getServletContext(), RHIZOMATIC_REST + "_" + holder.rootPath);
        }

    }

    private class Holder {
        String rootPath;
        ServletContainer servletContainer;
        ResourceConfig resourceConfig;

        public Holder(String rootPath, ServletContainer servletContainer, ResourceConfig resourceConfig) {
            this.rootPath = rootPath;
            this.servletContainer = servletContainer;
            this.resourceConfig = resourceConfig;
        }
    }

}
