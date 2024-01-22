import io.rhizomatic.inject.InjectionSubsystem;
import io.rhizomatic.kernel.spi.subsystem.Subsystem;

/**
 * Provides service assembly and injection based on Guice.
 */
module io.rhizomatic.inject {

    requires io.rhizomatic.api;
    requires io.rhizomatic.kernel;
    requires com.google.guice;
    requires static org.jetbrains.annotations;

    provides Subsystem with InjectionSubsystem;
}
