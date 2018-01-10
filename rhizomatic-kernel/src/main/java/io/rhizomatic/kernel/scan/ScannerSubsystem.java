package io.rhizomatic.kernel.scan;

import io.rhizomatic.kernel.spi.subsystem.Subsystem;
import io.rhizomatic.kernel.spi.subsystem.SubsystemContext;

/**
 *
 */
public class ScannerSubsystem extends Subsystem {

    public ScannerSubsystem() {
        super("rhizomatic.scanner");
    }

    public void instantiate(SubsystemContext context) {
        IntrospectionService introspectionService = new IntrospectionService(context);

        ClassScanner classScanner = new ClassScanner(introspectionService);
        context.registerService(ClassScanner.class, classScanner);
    }

}
