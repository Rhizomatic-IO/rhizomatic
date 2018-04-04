package io.rhizomatic.inject;

import io.rhizomatic.inject.guice.GuiceInstanceManager;
import io.rhizomatic.inject.scan.InjectionIntrospector;
import io.rhizomatic.kernel.spi.inject.InstanceManager;
import io.rhizomatic.kernel.spi.scan.Introspector;
import io.rhizomatic.kernel.spi.subsystem.Subsystem;
import io.rhizomatic.kernel.spi.subsystem.SubsystemContext;

import java.util.Set;

/**
 * Loads the Injection subsystem. Provides service injection and wiring using Guice.
 */
public class InjectionSubsystem extends Subsystem {
    private static final Set<String> OPENS = Set.of("com.google.guice", "io.rhizomatic.inject");
    private GuiceInstanceManager instanceManager;

    public InjectionSubsystem() {
        super("rhizomatic.injection");
    }

    public String getName() {
        return "rhizomatic.injection";
    }

    public Set<String> openModulesTo() {
        return OPENS;
    }

    public void instantiate(SubsystemContext context) {
        InjectionIntrospector introspector = new InjectionIntrospector();
        context.registerService(Introspector.class, introspector);

        instanceManager = new GuiceInstanceManager();
        context.registerService(InstanceManager.class, instanceManager);
    }

    public void applicationInitialize(SubsystemContext context) {
        instanceManager.startInstances();
    }

    public void shutdown() {
        instanceManager = null;
    }

}
