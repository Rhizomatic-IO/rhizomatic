package io.rhizomatic.kernel.spi.scan;

/**
 * Introspects a type in a module.
 */
public interface Introspector {

    /**
     * Introspects the type, possibly binding it in the index
     *
     * @param type the type
     * @param builder the index builder
     */
    void introspect(Class<?> type, ScanIndex.Builder builder);

}
