import io.rhizomatic.inject.InjectionSubsystem;
import io.rhizomatic.kernel.spi.subsystem.Subsystem;

/**
 * Provides service assembly and injection based on Guice.
 */
module io.rhizomatic.inject {

    exports io.rhizomatic.inject.api;

    requires io.rhizomatic.api;
    requires io.rhizomatic.kernel;
    requires javax.inject;
    requires com.google.guice;
    requires static annotations;

    uses com.google.inject.Module;

    provides Subsystem with InjectionSubsystem;
}
