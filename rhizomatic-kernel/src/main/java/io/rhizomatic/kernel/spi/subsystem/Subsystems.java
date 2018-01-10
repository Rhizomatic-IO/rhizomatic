package io.rhizomatic.kernel.spi.subsystem;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Manages subsystems that are programmatically installed via a client such as a test fixture.
 */
public final class Subsystems {
    static Set<Subsystem> INSTALLED = new LinkedHashSet<>();

    /**
     * Installs the subsystem.
     */
    public static void install(Subsystem subsystem) {
        INSTALLED.add(subsystem);
    }

    /**
     * Resets and clears installed subsystems. Intended to be used when the system needs to be booted multiple times with different configurations in the same classloader.
     */
    public static void reset() {
        INSTALLED.clear();
    }

    /**
     * Returns the installed subsystems.
     */
    public static Set<Subsystem> getInstalled() {
        return INSTALLED;
    }

    private Subsystems() {
    }
}
