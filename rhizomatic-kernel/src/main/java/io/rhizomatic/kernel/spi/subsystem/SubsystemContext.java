package io.rhizomatic.kernel.spi.subsystem;

import io.rhizomatic.api.Monitor;
import io.rhizomatic.api.web.WebApp;
import io.rhizomatic.kernel.layer.LoadedLayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Context used by subsystems to register and resolve SPI services. Service resolution can be performed at or after the subsystem has begun assembly.
 */
public interface SubsystemContext {

    /**
     * Returns the system monitor. Available in all normal system states.
     */
    Monitor getMonitor();

    /**
     * Returns the system layer configuration.
     */
    List<LoadedLayer> getLoadedLayers();

    /**
     * Returns the web app configuration.
     */
    List<WebApp> getWebApps();

    /**
     * Returns the configuration value for the type or null if not found. Available in all normal system states.
     *
     * @param type the expected value type.
     * @param key the key
     */
    @Nullable <T> T getConfiguration(Class<T> type, String key);

    /**
     * Registers an SPI service. Services should be registered during the intialization state.
     *
     * @param type the SPI contract type.
     * @param service the service
     */
    <T> void registerService(Class<T> type, T service);

    /**
     * Resolves the service for the given SPI type. If a service is not found, a runtime exception is thrown.
     */
    <T> T resolve(Class<T> type);

    /**
     * Resolves all services for the given SPI type. If a service is not found, a runtime exception is thrown.
     */
    <T> List<T> resolveAll(Class<T> type);


}
