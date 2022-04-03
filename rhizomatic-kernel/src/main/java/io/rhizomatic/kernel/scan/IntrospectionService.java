package io.rhizomatic.kernel.scan;

import io.rhizomatic.kernel.spi.scan.Introspector;
import io.rhizomatic.kernel.spi.scan.ScanIndex;
import io.rhizomatic.kernel.spi.subsystem.SubsystemContext;

import java.util.List;


/**
 * Introspects a collection of classes by delegating to {@link Introspector}s.
 */
public class IntrospectionService {
    private SubsystemContext context;

    public IntrospectionService(SubsystemContext context) {
        this.context = context;
    }

    void introspect(List<Class<?>> classes, ScanIndex.Builder builder) {
        var introspectors = context.resolveAll(Introspector.class);
        for (var clazz : classes) {
            for (var introspector : introspectors) {
                introspector.introspect(clazz, builder);
            }
        }
    }

}
