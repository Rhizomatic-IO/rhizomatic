package io.rhizomatic.kernel.spi.inject;

import io.rhizomatic.kernel.spi.scan.ScanIndex;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Manages injected instances.
 */
public interface InstanceManager {

    /**
     * Register an instance.
     *
     * @param type the type to bind the instance to
     * @param instance the instance
     */
    void register(Class<?> type, Object instance);

    /**
     * Wires instances specified in the index by binding and injecting them.
     *
     * @param scanIndex the index
     */
    void wire(ScanIndex scanIndex);

    /**
     * Starts eager instances.
     */
    void startInstances();

    /**
     * Resolves an instance bound to the type.
     *
     * @param type the type
     */
    @Nullable <T> T resolve(Class<T> type);

    /**
     * Resolves all instances bound to the type.
     *
     * @param type the type
     */
    <T> Set<T> resolveAll(Class<T> type);

    /**
     * Resolves all types associated with the qualifier.
     *
     * @param qualifier the qualifier, such as an annotation type. Note the resolved instances may not implement the qualifier type, e.g. if the qualifier is an annotation.
     */
    Set<?> resolveQualifiedTypes(Class<?> qualifier);

}
