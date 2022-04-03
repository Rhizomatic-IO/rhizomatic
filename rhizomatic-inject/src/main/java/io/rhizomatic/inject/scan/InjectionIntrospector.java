package io.rhizomatic.inject.scan;

import io.rhizomatic.api.annotations.Eager;
import io.rhizomatic.api.annotations.Init;
import io.rhizomatic.api.annotations.Service;
import io.rhizomatic.kernel.spi.scan.Introspector;
import io.rhizomatic.kernel.spi.scan.ScanIndex;

import static java.lang.reflect.Modifier.isAbstract;

/**
 * Introspects module classes for services and registers them with the scan index.
 */
public class InjectionIntrospector implements Introspector {
    public void introspect(Class<?> type, ScanIndex.Builder builder) {
        if (isAbstract(type.getModifiers())) {
            return;
        }

        var serviceAnnotation = type.getAnnotation(Service.class);

        if (serviceAnnotation == null) {
            return;
        }

        builder.service(type);

        if (type.getAnnotation(Eager.class) != null) {
            builder.eager(type);
        }

        // introspect for @Init - only support public methods
        for (var method : type.getMethods()) {
            if (method.getAnnotation(Init.class) != null) {
                builder.initCallback(type, method);
            }
        }
    }
}
