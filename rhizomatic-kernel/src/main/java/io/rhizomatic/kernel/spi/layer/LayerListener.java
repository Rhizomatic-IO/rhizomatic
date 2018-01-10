package io.rhizomatic.kernel.spi.layer;

import io.rhizomatic.api.layer.RzLayer;

import java.util.List;

/**
 * Receives lifecycle callbacks as layers are loaded in the system.
 */
public interface LayerListener {

    /**
     * Invoked before the layer is loaded in the system. Implementations may modify the configuration.
     */
    default void onLayerConfiguration(List<RzLayer> layers) {
    }

    /**
     * Invoked after a layer has been loaded.
     */
    default void onLayerLoaded(ModuleLayer.Controller controller) {
    }

    /**
     * Invoked after a module has been loaded.
     */
    default void onModuleLoaded(Module module) {
    }
}
