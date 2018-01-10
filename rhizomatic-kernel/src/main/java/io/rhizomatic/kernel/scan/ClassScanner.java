package io.rhizomatic.kernel.scan;

import io.rhizomatic.kernel.spi.scan.ScanIndex;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Scans and indexes a collection of classes.
 */
public class ClassScanner {
    private IntrospectionService introspectionService;

    private List<Class<?>> cache = new ArrayList<>();

    public ClassScanner(IntrospectionService introspectionService) {
        this.introspectionService = introspectionService;
    }

    /**
     * Adds a class to scan.
     *
     * @param clazz the class to scan
     */
    public void addClass(Class<?> clazz) {
        Objects.requireNonNull(clazz, "Class to scan was null");
        if (cache.contains(clazz)) {
            return;
        }
        cache.add(clazz);
    }

    /**
     * Performs the scan and returns an index.
     */
    public ScanIndex scan() {
        ScanIndex.Builder builder = ScanIndex.Builder.newInstance();
        introspectionService.introspect(cache, builder);
        return builder.build();
    }
}
