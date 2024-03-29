package io.rhizomatic.kernel.scan;

import io.rhizomatic.api.RhizomaticException;
import io.rhizomatic.kernel.spi.layer.LoadedLayer;
import io.rhizomatic.kernel.spi.scan.ScanIndex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static io.rhizomatic.kernel.spi.util.ClassHelper.getClassName;
import static java.util.Objects.requireNonNull;

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
     * Performs the scan on a set of layers and returns an index.
     *
     * @param loadedLayers the layers to scan
     */
    public ScanIndex scan(List<LoadedLayer> loadedLayers) {
        for (var loadedLayer : loadedLayers) {
            for (var reference : loadedLayer.getReferences()) {
                try {
                    reference.open().list().forEach(fileName -> {
                        if (fileName.endsWith(".class") && !fileName.startsWith("module-info") && !fileName.startsWith("package-info")) {
                            try {
                                var module = loadedLayer.getModule(reference);
                                // Important: load using the module classloader so the class is loaded using the module
                                var type = module.getClassLoader().loadClass(getClassName(fileName));
                                addClass(type);
                            } catch (ClassNotFoundException e) {
                                throw new RhizomaticException("Error loading module: " + reference.descriptor().name(), e);
                            }
                        }
                    });
                } catch (IOException e) {
                    throw new RhizomaticException("Error loading module: " + reference.descriptor().name(), e);
                }
            }
        }

        var builder = ScanIndex.Builder.newInstance();
        builder.layers(loadedLayers);
        introspectionService.introspect(cache, builder);
        return builder.build();
    }

    /**
     * Performs the scan on a set of classes and returns an index.
     *
     * @param classes the classes to scan
     */
    public ScanIndex scan(Set<Class<?>> classes) {
        reset();
        classes.forEach(this::addClass);
        var builder = ScanIndex.Builder.newInstance();
        introspectionService.introspect(cache, builder);
        return builder.build();
    }

    /**
     * Adds a class to scan.
     *
     * @param clazz the class to scan
     */
    private void addClass(Class<?> clazz) {
        requireNonNull(clazz, "Class to scan was null");
        if (cache.contains(clazz)) {
            return;
        }
        cache.add(clazz);
    }

    private void reset() {
        cache.clear();
    }

}
