package io.rhizomatic.web.api;

import io.rhizomatic.kernel.spi.subsystem.Subsystems;
import io.rhizomatic.web.WebSubsystem;

/**
 * Installs the Web extension in test and classpath-based environments.
 */
public class WebModule {

    /**
     * Enables the extension.
     */
    public static void install() {
        Subsystems.install(new WebSubsystem());
    }

    private WebModule() {
    }
}
