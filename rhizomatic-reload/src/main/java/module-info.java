import io.rhizomatic.kernel.spi.subsystem.Subsystem;
import io.rhizomatic.reload.jrebel.JRebelSubsystem;

/**
 *
 */
module io.rhizomatic.reload {

    requires io.rhizomatic.api;
    requires io.rhizomatic.kernel;
    requires static jr.sdk;      // static since JRebel is not on the module path
    requires static jr.utils;    // static since JRebel is not on the module path

    provides Subsystem with JRebelSubsystem;

}