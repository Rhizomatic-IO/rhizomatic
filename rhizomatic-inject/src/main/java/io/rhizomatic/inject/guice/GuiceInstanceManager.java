package io.rhizomatic.inject.guice;

import com.google.inject.AbstractModule;
import com.google.inject.ConfigurationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.util.Types;
import io.rhizomatic.api.RhizomaticException;
import io.rhizomatic.kernel.spi.inject.InstanceManager;
import io.rhizomatic.kernel.spi.layer.LoadedLayer;
import io.rhizomatic.kernel.spi.scan.ScanIndex;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Guice implementation of an instance manager.
 */
public class GuiceInstanceManager implements InstanceManager {
    private Map<Class<?>, Object> instances = new HashMap<>();
    private Set<Class<?>> eagerServices = Collections.emptySet();
    private Map<Class<?>, Set<Class<?>>> qualifiedServices = Collections.emptyMap();
    private Injector injector;

    private boolean wired = false;

    public GuiceInstanceManager() {
    }

    public void register(Class<?> type, Object instance) {
        instances.put(type, instance);
    }

    public void wire(ScanIndex scanIndex) {
        AbstractModule injectModule = new AbstractModule() {
            @SuppressWarnings("unchecked")
            protected void configure() {
                if (!scanIndex.getInitCallbacks().isEmpty()) {
                    bindListener(Matchers.any(), new LifecycleListener(scanIndex.getInitCallbacks()));
                }

                for (Map.Entry<Class<?>, Object> entry : instances.entrySet()) {
                    Class key = entry.getKey();
                    bind(key).toInstance(entry.getValue());
                }

                // install provided modules
                if (!scanIndex.getLayers().isEmpty()) {
                    for (LoadedLayer loadedLayer : scanIndex.getLayers()) {
                        ModuleLayer layer = loadedLayer.getModuleLayer();
                        ServiceLoader<com.google.inject.Module> guiceModules = ServiceLoader.load(layer, com.google.inject.Module.class);
                        guiceModules.forEach(this::install);
                    }
                }

                // bind scanned services
                for (Map.Entry<Class<?>, List<Class<?>>> entry : scanIndex.getServiceBindings().entrySet()) {
                    if (entry.getValue().isEmpty()) {
                        //noinspection UnnecessaryContinue
                        continue;
                    } else if (entry.getValue().size() == 1) {
                        Class implClass = entry.getValue().get(0);
                        bind(entry.getKey()).to(implClass).in(Scopes.SINGLETON);
                    } else {
                        Multibinder builder = Multibinder.newSetBinder(binder(), entry.getKey());
                        // order the multi-bindings are loaded in the module determines injection order
                        for (Class<?> implClass : entry.getValue()) {
                            builder.addBinding().to(implClass).in(Scopes.SINGLETON);
                            bind(implClass).in(Scopes.SINGLETON);    // force singleton .cf https://github.com/google/guice/issues/791
                        }
                    }
                }
                eagerServices = scanIndex.getEagerServices();
                qualifiedServices = scanIndex.getQualifiedServices();
            }
        };
        injector = Guice.createInjector(injectModule);
        wired = true;
    }

    public void startInstances() {
        checkWired();
        for (Class<?> eagerService : eagerServices) {
            injector.getInstance(eagerService); // TODO handle case where not bound to type
        }
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable T resolve(Class<T> type) {
        checkWired();
        try {
            return injector.getInstance(type);
        } catch (ConfigurationException e) {
            throw new RhizomaticException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Set<T> resolveAll(Class<T> type) {
        checkWired();
        TypeLiteral<Set<T>> literal = (TypeLiteral<Set<T>>) TypeLiteral.get(Types.setOf(type));
        try {
            return injector.getInstance(Key.get(literal));
        } catch (ConfigurationException e) {
            throw new RhizomaticException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public Set<?> resolveQualifiedTypes(Class<?> qualifier) {
        Set<Class<?>> implTypes = qualifiedServices.get(qualifier);
        if (implTypes == null) {
            return Collections.emptySet();
        }
        Set<Object> instances = new LinkedHashSet<>();
        for (Class implType : implTypes) {
            Object resolved = resolve(implType);
            if (resolved != null) {
                instances.add(resolved);
            }
        }
        return instances;
    }

    private void checkWired() {
        if (!wired) {
            throw new IllegalStateException(getClass().getName() + " not wired");
        }
    }
}
