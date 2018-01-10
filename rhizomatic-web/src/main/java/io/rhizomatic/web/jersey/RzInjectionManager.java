package io.rhizomatic.web.jersey;

import io.rhizomatic.api.Monitor;
import org.glassfish.jersey.internal.inject.Binder;
import org.glassfish.jersey.internal.inject.Binding;
import org.glassfish.jersey.internal.inject.ClassBinding;
import org.glassfish.jersey.internal.inject.ForeignDescriptor;
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

/**
 * Bridges Jersey injection and the system injection manager.
 */
public class RzInjectionManager implements InjectionManager {
    // List of contracts to instances registered by Jersey. Jersey registers instances it creates via Service locators as well as instances resolved from the Guice
    // injection manager.
    private Map<Object, List<InstanceHolder>> holders = new HashMap<>();

    private Monitor monitor;

    public RzInjectionManager(Monitor monitor) {
        registerJerseyServices();
        this.monitor = monitor;
    }

    public void completeRegistration() {
    }

    public void shutdown() {
    }

    public void register(Binding binding) {
        Set<Annotation> qualifiers = binding.getQualifiers();
        if (binding instanceof InstanceBinding) {
            InstanceBinding<?> instanceBinding = (InstanceBinding) binding;
            for (Object contract : binding.getContracts()) {
                List<InstanceHolder> list = holders.computeIfAbsent(contract, k -> new ArrayList<>());
                InstanceHolder holder = new InstanceHolder(instanceBinding.getService(), qualifiers);
                list.add(holder);
            }
        } else if (binding instanceof ClassBinding) {
            ClassBinding classBinding = (ClassBinding) binding;
            for (Object contract : binding.getContracts()) {
                try {
                    if (contract.equals(RequestScope.class)) {
                        continue;
                    }
                    List<InstanceHolder> list = holders.computeIfAbsent(contract, k -> new ArrayList<>());
                    Class<?> serviceClass = classBinding.getService();
                    InstanceHolder holder = new InstanceHolder(serviceClass.getConstructor().newInstance(), qualifiers);
                    list.add(holder);
                } catch (Throwable e) {
                    // FIXME uncomment and remove all of this
//                    monitor.info(() -> "JAX-RS Type: " + classBinding.getService().getName());
//                    e.printStackTrace();
                }
            }
        } else if (binding instanceof SupplierInstanceBinding) {
            SupplierInstanceBinding supplierBinding = (SupplierInstanceBinding) binding;
            for (Object contract : binding.getContracts()) {
                List<InstanceHolder> list = holders.computeIfAbsent(contract, k -> new ArrayList<>());
                InstanceHolder holder = new InstanceHolder(supplierBinding.getSupplier().get(), qualifiers);
                list.add(holder);
            }
        }

    }

    public void register(Iterable<Binding> descriptors) {
        for (Binding descriptor : descriptors) {
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

    public <T> T createAndInitialize(Class<T> implClass) {
        try {
            return implClass.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> List<ServiceHolder<T>> getAllServiceHolders(Class<T> contract, Annotation... qualifiers) {
        List<InstanceHolder> list = holders.get(contract);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<ServiceHolder<T>> serviceHolders = new ArrayList<>();
        for (InstanceHolder holder : list) {
            if (qualifiers == null) {
                ServiceHolder<T> serviceHolder = new ServiceHolderImpl<T>((T) holder.instance, Set.of(contract));
                serviceHolders.add(serviceHolder);
            } else {
                for (Annotation qualifier : qualifiers) {
                    if (holder.qualifiers.contains(qualifier)) {
                        ServiceHolder<T> serviceHolder = new ServiceHolderImpl<T>((T) holder.instance, Set.of(contract));
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
        return getInstance((Type) contract);
    }

    @SuppressWarnings("unchecked")
    public <T> T getInstance(Type contract) {
        List<InstanceHolder> list = holders.get(contract);
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
        List<T> list = new ArrayList<>();
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

    private void registerJerseyServices() {
        List<InstanceHolder> list = new ArrayList<>();
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

    private class InstanceHolder {
        private Set<Annotation> qualifiers;
        private Object instance;

        public InstanceHolder(Object instance, Set<Annotation> qualifiers) {
            this.qualifiers = qualifiers;
            this.instance = instance;
        }
    }


}
