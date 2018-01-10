package io.rhizomatic.kernel.spi.scan;

import io.rhizomatic.api.annotations.Service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An index of scanned types.
 */
public class ScanIndex {
    private static final Comparator<Class<?>> ORDER_COMPARATOR = (c1, c2) -> {
        Service a1 = c1.getAnnotation(Service.class);
        Service a2 = c2.getAnnotation(Service.class);
        int order1 = a1 != null ? a1.order() : Integer.MIN_VALUE;
        int order2 = a2 != null ? a2.order() : Integer.MIN_VALUE;
        return order1 - order2;
    };

    private Map<Class<?>, List<Class<?>>> bindingToServices = new HashMap<>();

    private Set<Class<?>> eagerServices = new HashSet<>();
    private Map<Class<?>, Set<Class<?>>> serviceQualifiers = new HashMap<>();  // service to qualifiers
    private Map<Class<?>, Set<Class<?>>> qualifiedServices = new HashMap<>();  // qualifiers to service
    private Map<Class<?>, Method> initCallbacks = new HashMap<>();

    private List<Problem> problems = new ArrayList<>();

    public Map<Class<?>, List<Class<?>>> getServiceBindings() {
        return bindingToServices;
    }

    public Map<Class<?>, Set<Class<?>>> getQualifiedServices() {
        return qualifiedServices;
    }

    public Set<Class<?>> getEagerServices() {
        return eagerServices;
    }

    public Map<Class<?>, Method> getInitCallbacks() {
        return initCallbacks;
    }

    public List<Problem> getProblems() {
        return problems;
    }

    public boolean hasErrors() {
        for (Problem problem : problems) {
            if (Problem.Type.ERROR == problem.getType()) {
                return true;
            }
        }
        return false;
    }

    private ScanIndex() {
    }

    public static class Builder {
        private ScanIndex index;
        private Set<Class> seenServices = new HashSet<>();

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder service(Class<?> service) {
            if (seenServices.contains(service)) {
                return this;
            }
            seenServices.add(service);

            Service annotation = service.getAnnotation(Service.class);
            if (annotation != null) {
                // TODO support profiles by ignoring services not in profile
                Class<?>[] bindings = annotation.values();
                if (bindings.length == 1 && Void.class.equals(bindings[0])) {
                    // no service interface specified, bind to impl
                    List<Class<?>> list = index.bindingToServices.computeIfAbsent(service.getClass(), (k) -> new ArrayList<>());
                    list.add(service);
                    if (service.getInterfaces().length == 1 && !service.getInterfaces()[0].getPackageName().startsWith("java.")) {
                        // also bind the interface if a single one
                        list = index.bindingToServices.computeIfAbsent(service.getInterfaces()[0], (k) -> new ArrayList<>());
                        list.add(service);
                    }
                } else {
                    for (Class<?> binding : bindings) {
                        List<Class<?>> list = index.bindingToServices.computeIfAbsent(binding, (k) -> new ArrayList<>());
                        list.add(service);
                    }
                }
            }
            return this;
        }

        public Builder eager(Class<?> service) {
            index.eagerServices.add(service);
            return this;
        }

        public Builder qualified(Class<?> service, Class<?> qualifier) {
            Set<Class<?>> services = index.qualifiedServices.computeIfAbsent(qualifier, (k) -> new HashSet<>());
            services.add(service);

            Set<Class<?>> qualifiers = index.serviceQualifiers.computeIfAbsent(service, (k) -> new HashSet<>());
            qualifiers.add(qualifier);
            return this;
        }

        public Builder initCallback(Class<?> service, Method method) {
            index.initCallbacks.put(service, method);
            return this;
        }

        public Builder problem(Problem problem) {
            index.problems.add(problem);
            return this;
        }

        public ScanIndex build() {
            for (Map.Entry<Class<?>, List<Class<?>>> entry : index.bindingToServices.entrySet()) {
                if (entry.getValue().size() < 2) {
                    continue;
                }
                // sort ordered services
                boolean sort = entry.getValue().stream().anyMatch((Class<?> service) -> {
                    Service annotation = service.getAnnotation(Service.class);
                    return annotation != null && annotation.order() > Integer.MIN_VALUE;
                });
                if (sort) {
                    entry.getValue().sort(ORDER_COMPARATOR);
                }

            }
            return index;
        }

        private Builder() {
            index = new ScanIndex();
        }

    }
}
