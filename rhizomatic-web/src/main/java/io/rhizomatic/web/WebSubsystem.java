package io.rhizomatic.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
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
import io.rhizomatic.web.http.RewriteHandler;
import io.rhizomatic.web.jersey.RzInjectionManager;
import io.rhizomatic.web.jersey.RzInjectionManagerFactory;
import io.rhizomatic.web.scan.WebIntrospector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.Source;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.glassfish.jersey.message.internal.MessagingBinders;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.servlet.internal.Utils;

import javax.ws.rs.Path;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
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

    private Map<Object, Holder> instanceToContainers = new ConcurrentHashMap<>();  // instance to the Jersey servlet they are running in
    private Map<Object, Holder> pathToContainers = new ConcurrentHashMap<>();  // root path (web path) to the Jersey servlet that contains the endpoints

    public WebSubsystem() {
        super("rhizomatic.web");
    }

    public Set<String> openModulesTo() {
        return OPENS;
    }

    public void instantiate(SubsystemContext context) {
        monitor = context.getMonitor();
        RzInjectionManagerFactory.INSTANCE = new RzInjectionManager();
        RzInjectionManagerFactory.INSTANCE.register(new MessagingBinders.MessageBodyProviders(Collections.emptyMap(), RuntimeType.SERVER));

        jettyTransport = new JettyTransport();
        jettyTransport.initialize(context);
        context.registerService(JettyTransport.class, jettyTransport);

        WebIntrospector introspector = new WebIntrospector();
        context.registerService(Introspector.class, introspector);
        monitor.info(() -> "Web subsystem enabled");
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

            JacksonJsonProvider jacksonJsonProvider = createJacksonProvider(instanceManager);

            resourceConfig.registerInstances(jacksonJsonProvider);
            resourceConfig.register(endpoint);
        }
        return resourceConfigs;
    }

    protected void exportResources(Map<String, ResourceConfig> configurations) {
        for (Map.Entry<String, ResourceConfig> entry : configurations.entrySet()) {
            String rootPath = entry.getKey();

            ResourceConfig resourceConfig = entry.getValue();

            ServletContainer servletContainer = new ServletContainer();
            Holder holder = new Holder(rootPath, servletContainer, resourceConfig);
            pathToContainers.put(rootPath, holder); // track the root path and container for reloading (resource additions)
            for (Object instance : resourceConfig.getSingletons()) {
                // track the instance and the container it is running in for reloading
                instanceToContainers.put(instance, holder);
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
            RewriteHandler rewriteHandler = new RewriteHandler();

            ResourceHandler resourceHandler = new ResourceHandler();
            resourceHandler.setBaseResource(resources);

            rewriteHandler.setHandler(resourceHandler);

            ContextHandler ctx = new ContextHandler(contextPath); /* the server uri path */
            ctx.setHandler(rewriteHandler);

            jettyTransport.registerHandler(ctx);

            monitor.info(() -> "Web app at: " + (contextPath.startsWith("/") ? contextPath : "/" + contextPath));
        }
    }

    /**
     * Creates a Jackson provider that can retrieve an application-provided ObjectMapper.
     */
    private JacksonJsonProvider createJacksonProvider(InstanceManager instanceManager) {
        return new JacksonJsonProvider() {
            @SuppressWarnings("unchecked")
            // overrides the mapper locator method to resolve the instance from the instance manager
            protected ObjectMapper _locateMapperViaProvider(Class<?> type, MediaType mediaType) {
                Set<?> providers = instanceManager.resolveQualifiedTypes(Provider.class);
                for (Object provider : providers) {
                    if (provider instanceof ContextResolver) {
                        for (Type interfaze : provider.getClass().getGenericInterfaces()) {
                            if (interfaze instanceof ParameterizedType) {
                                ParameterizedType parameterizedType = (ParameterizedType) interfaze;
                                if (parameterizedType.getRawType().equals(ContextResolver.class)) {
                                    if (parameterizedType.getActualTypeArguments()[0].equals(ObjectMapper.class)) {
                                        return (ObjectMapper) ((ContextResolver) provider).getContext(Object.class);
                                    }
                                }
                            }
                        }
                    }
                }
                return super._locateMapperViaProvider(type, mediaType);
            }
        };
    }

    /**
     * Listener to reload resource endpoints.
     */
    private class WebReloadListener implements ReloadListener {

        public void onInstanceChanged(Object instance) {
            if (notJaxRS(instance)) {
                return;
            }
            Holder holder = instanceToContainers.get(instance);
            if (holder == null) {
                // if the instance is not tracked, it could be that is did not have JAX-RS annotations prior to the change
                onInstanceAdded(instance);
                return;
            }
            setContext(holder);

        }

        public void onInstanceAdded(Object instance) {
            if (notJaxRS(instance)) {
                return;
            }
            String rootPath = getRootPath(instance);

            Holder holder = pathToContainers.get(rootPath);
            if (holder == null) {
                // TODO support adding new context?
                return;
            }
            setContext(holder);
            if (!holder.resourceConfig.isRegistered(instance)) {
                holder.resourceConfig.register(instance);
                // passing null is important otherwise JRebel throws an error; the ResourceConfig will be pulled from the servlet context
                holder.servletContainer.reload(null);
            }
        }

        private void setContext(Holder holder) {
            // The resource context needs to be reset so it is available when the web context is reloaded; Utils.store() removes the context
            // when it is called so it is no longer in the servlet context after the initial load has completed.
            Utils.store(holder.resourceConfig, holder.servletContainer.getServletContext(), RHIZOMATIC_REST + "_" + holder.rootPath);
        }

    }

    private boolean notJaxRS(Object instance) {
        Class<?> clazz = instance.getClass();
        return clazz.getAnnotation(Path.class) == null && clazz.getAnnotation(Provider.class) == null;
    }

    private String getRootPath(Object instance) {
        Class<?> clazz = instance.getClass();
        Module module = clazz.getModule();
        String rootPath = "api";
        if (module != null && module.getAnnotation(EndpointPath.class) != null) {
            rootPath = module.getAnnotation(EndpointPath.class).value();
        }
        return rootPath;
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
