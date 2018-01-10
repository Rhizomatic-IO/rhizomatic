package io.rhizomatic.kernel.layer;

import io.rhizomatic.kernel.spi.subsystem.Subsystem;
import io.rhizomatic.kernel.spi.subsystem.SubsystemContext;

/**
 *
 */
public class LayerSubsystem extends Subsystem {
    private LayerManager layerManager;

    public LayerSubsystem() {
        super("rhizomatic.layer");
    }

    public void instantiate(SubsystemContext context) {
        layerManager = new LayerManager();
        context.registerService(LayerManager.class, layerManager);
    }

    public void shutdown() {
        if (layerManager != null) {
            layerManager.release();
            layerManager = null;
        }
    }
}
