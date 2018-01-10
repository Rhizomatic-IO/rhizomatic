package io.rhizomatic.inject.api;

import io.rhizomatic.inject.InjectionSubsystem;
import io.rhizomatic.kernel.spi.subsystem.Subsystems;

/**
 * Installs the Injection extension in test and classpath-based environments.
 */
public class InjectionModule {

    /**
     * Enables the extension.
     */
    public static void install() {
        Subsystems.install(new InjectionSubsystem());
    }

    private InjectionModule() {
    }
}
