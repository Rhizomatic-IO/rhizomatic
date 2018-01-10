package io.rhizomatic.web.jersey;

import io.rhizomatic.api.Monitor;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.InjectionManagerFactory;

/**
 * Attaches the injection manager backed by the system instance manager to Jersey.
 */
public class RzInjectionManagerFactory implements InjectionManagerFactory {
    public static Monitor MONITOR;

    public InjectionManager create() {
        return new RzInjectionManager(MONITOR);
    }

    public InjectionManager create(Object parent) {
        return create();
    }
}
