package io.rhizomatic.web.jersey;

import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.InjectionManagerFactory;

/**
 * Attaches the injection manager backed by the system instance manager to Jersey.
 */
public class RzInjectionManagerFactory implements InjectionManagerFactory {
    public static RzInjectionManager INSTANCE;

    public InjectionManager create() {
        return INSTANCE;
    }

    public InjectionManager create(Object parent) {
        return create();
    }
}
