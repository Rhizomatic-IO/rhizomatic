package io.rhizomatic.web.jersey;

import org.glassfish.jersey.internal.inject.Binder;
import org.glassfish.jersey.internal.inject.Binding;
import org.glassfish.jersey.internal.inject.ClassBinding;
import org.glassfish.jersey.internal.inject.ContextInjectionResolver;
import org.glassfish.jersey.internal.inject.ForeignDescriptor;
import org.glassfish.jersey.internal.inject.Injectee;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.InstanceBinding;
import org.glassfish.jersey.internal.inject.ServiceHolder;
import org.glassfish.jersey.internal.inject.ServiceHolderImpl;
import org.glassfish.jersey.internal.inject.SupplierInstanceBinding;
import org.glassfish.jersey.process.internal.RequestContext;
import org.glassfish.jersey.process.internal.RequestScope;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.Context;

import static io.rhizomatic.kernel.spi.util.Cast.cast;
import static java.lang.ThreadLocal.withInitial;
import static java.util.Objects.requireNonNull;

/**
 * Bridges Jersey injection and the system injection manager.
 * <p>
 * Implements {@code ContextInjectionResolver} to support injection of custom types annotated with {@code @Context} on controller classes.
 */
public class RzInjectionManager implements InjectionManager, ContextInjectionResolver {
    private static final String CUSTOM_ANNOTATION = "org.glassfish.jersey.internal.inject.Custom";

    // List of contracts to instances registered by Jersey. Jersey registers instances it creates via Service locators
    // as well as instances resolved from the Guice injection manager.
    private Map<Object, List<InstanceHolder>> holders = new HashMap<>();

    private ThreadLocal<Map<Type, Object>> contextInstances = withInitial(HashMap::new);

    public RzInjectionManager() {
        registerJerseyServices();
    }

    public void completeRegistration() {
    }

    public void shutdown() {
    }

    /**
     * Registers an instance scoped to the current request. Used for injection of custom {@code @Context} objects.
     */
    public <T> void registerRequestInstance(T instance, Class<T> type) {
        requireNonNull(type);
        var context = contextInstances.get();
        context.put(type, instance);
    }

    /**
     * Removes request-scoped instances.
     */
    public void clearRequestInstances() {
        contextInstances.remove();
    }

    public void register(Binding binding) {
        var qualifiers = binding.getQualifiers();
        if (binding instanceof InstanceBinding) {
            @SuppressWarnings("rawtypes") var instanceBinding = (InstanceBinding) binding;
            for (var contract : binding.getContracts()) {
                var list = holders.computeIfAbsent(contract, k -> new ArrayList<>());
                @SuppressWarnings("unchecked") var holder = new InstanceHolder(instanceBinding.getService(), qualifiers);
                list.add(holder);
            }
        } else if (binding instanceof ClassBinding) {
            @SuppressWarnings("rawtypes") var classBinding = (ClassBinding) binding;
            for (var contract : binding.getContracts()) {
                try {
                    if (contract.equals(RequestScope.class)) {
                        continue;
                    }
                    var list = holders.computeIfAbsent(contract, k -> new ArrayList<>());
                    var serviceClass = classBinding.getService();
                    @SuppressWarnings("unchecked") var holder = new InstanceHolder(serviceClass.getConstructor().newInstance(), qualifiers);
                    list.add(holder);
                } catch (Throwable e) {
                    // ignore WADL errors
                }
            }
        } else if (binding instanceof SupplierInstanceBinding) {
            @SuppressWarnings("rawtypes") var supplierBinding = (SupplierInstanceBinding) binding;
            for (var contract : binding.getContracts()) {
                var list = holders.computeIfAbsent(contract, k -> new ArrayList<>());
                @SuppressWarnings("unchecked") InstanceHolder holder = new InstanceHolder(supplierBinding.getSupplier().get(), qualifiers);
                list.add(holder);
            }
        }

    }

    public void register(Iterable<Binding> descriptors) {
        for (var descriptor : descriptors) {
            register(descriptor);
        }
    }

    public void register(Binder binder) {
        register(binder.getBindings());
    }

    public void register(Object provider) throws IllegalArgumentException {

    }

    public boolean isRegistrable(Class<?> clazz) {
        return false;
    }

    @Override
    public <T> T create(Class<T> createMe) {
        throw new UnsupportedOperationException();
    }

    public <T> T createAndInitialize(Class<T> implClass) {
        try {
            return implClass.getConstructor().newInstance();
        } catch (InstantiationException
                 | IllegalAccessException
                 | NoSuchMethodException
                 | InvocationTargetException e) {
            throw new RuntimeException("Error creating type: " + e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> List<ServiceHolder<T>> getAllServiceHolders(Class<T> contract, Annotation... qualifiers) {
        if (qualifiers != null && qualifiers.length == 1 && CUSTOM_ANNOTATION.equals(qualifiers[0].annotationType().getName())) {
            // ignore the custom annotation
            qualifiers = null;
        }
        var list = holders.get(contract);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        var serviceHolders = new ArrayList<ServiceHolder<T>>();
        for (var holder : list) {
            if (qualifiers == null) {
                var serviceHolder = new ServiceHolderImpl<>((T) holder.instance, Set.of(contract));
                serviceHolders.add(serviceHolder);
            } else {
                for (var qualifier : qualifiers) {
                    if (holder.qualifiers.contains(qualifier)) {
                        ServiceHolder<T> serviceHolder = new ServiceHolderImpl<>((T) holder.instance, Set.of(contract));
                        serviceHolders.add(serviceHolder);
                        break;
                    }
                }

            }
        }
        return serviceHolders;
    }

    public <T> T getInstance(Class<T> contract, Annotation... qualifiers) {
        return getInstance(contract);
    }

    public <T> T getInstance(Class<T> contract, String classAnalyzer) {
        return getInstance(contract);
    }

    public <T> T getInstance(Class<T> contract) {
        if (ContextInjectionResolver.class.equals(contract)) {
            return cast(this);
        }
        return getInstance((Type) contract);
    }

    @SuppressWarnings("unchecked")
    public <T> T getInstance(Type contract) {
        var list = holders.get(contract);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return (T) list.get(0).instance;
    }

    public Object getInstance(ForeignDescriptor descriptor) {
        return null;
    }

    public ForeignDescriptor createForeignDescriptor(Binding binding) {
        return null;
    }

    public <T> List<T> getAllInstances(Type contract) {
        T t = getInstance(contract);
        var list = new ArrayList<T>();
        if (t == null) {
            return list;
        }
        list.add(t);
        return list;
    }

    public void inject(Object instance) {
    }

    public void inject(Object instance, String classAnalyzer) {
    }

    public void preDestroy(Object instance) {
    }

    @Override
    public Object resolve(Injectee injectee) {
        var type = injectee.getRequiredType();
        return contextInstances.get().get(type);
    }

    @Override
    public boolean isConstructorParameterIndicator() {
        return false;
    }

    @Override
    public boolean isMethodParameterIndicator() {
        return true;
    }

    @Override
    public Class<Context> getAnnotation() {
        return Context.class;
    }

    private void registerJerseyServices() {
        var list = new ArrayList<InstanceHolder>();
        list.add(new InstanceHolder(new RzRequestScope(), Collections.emptySet()));
        holders.put(RequestScope.class, list);
    }

    private static class RzRequestScope extends RequestScope {

        public RequestContext createContext() {
            return new RequestContext() {
                public RequestContext getReference() {
                    return this;
                }

                public void release() {

                }
            };
        }
    }

    private static class InstanceHolder {
        private Set<Annotation> qualifiers;
        private Object instance;

        public InstanceHolder(Object instance, Set<Annotation> qualifiers) {
            this.qualifiers = qualifiers;
            this.instance = instance;
        }
    }


}
