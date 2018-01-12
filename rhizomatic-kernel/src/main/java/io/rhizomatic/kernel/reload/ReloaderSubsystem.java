package io.rhizomatic.kernel.reload;

import io.rhizomatic.kernel.scan.ClassScanner;
import io.rhizomatic.kernel.spi.inject.InstanceManager;
import io.rhizomatic.kernel.spi.reload.RzReloader;
import io.rhizomatic.kernel.spi.subsystem.Subsystem;
import io.rhizomatic.kernel.spi.subsystem.SubsystemContext;

/**
 *
 */
public class ReloaderSubsystem extends Subsystem {
    private RzReloaderImpl reloader;
    
    public ReloaderSubsystem() {
        super("rhizomatic.kernel.reload");
    }

    public void instantiate(SubsystemContext context) {
        reloader = new RzReloaderImpl();
        context.registerService(RzReloader.class, reloader);
    }

    public void assemble(SubsystemContext context) {
        InstanceManager instanceManager = context.resolve(InstanceManager.class);
        ClassScanner scanner = context.resolve(ClassScanner.class);
        reloader.initialize(instanceManager, scanner);
    }


}
