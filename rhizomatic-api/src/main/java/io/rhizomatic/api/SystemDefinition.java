package io.rhizomatic.api;

import io.rhizomatic.api.layer.RzLayer;
import io.rhizomatic.api.web.WebApp;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Provides a definition to launch the system.
 */
public interface SystemDefinition {

    /**
     * Returns the layer definition.
     */
    default List<RzLayer> getLayers() {
        return Collections.emptyList();
    }

    /**
     * Returns the configured web apps.
     */
    default List<WebApp> getWebApps() {
        return Collections.emptyList();
    }

    /**
     * Returns the system configuration.
     */
    default Map<String, String> getConfiguration() {
        return Collections.emptyMap();
    }
}
