import io.rhizomatic.kernel.spi.layer.LayerListener;
import io.rhizomatic.reload.jrebel.JRebelLayerListener;

/**
 *
 */
module io.rhizomatic.reload {


    requires io.rhizomatic.api;
    requires io.rhizomatic.kernel;
    requires jr.sdk;
    requires jr.utils;

    provides LayerListener with JRebelLayerListener;
}