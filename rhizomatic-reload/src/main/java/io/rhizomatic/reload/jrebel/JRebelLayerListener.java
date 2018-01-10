package io.rhizomatic.reload.jrebel;

import io.rhizomatic.kernel.spi.layer.LayerListener;
import org.zeroturnaround.javarebel.Integration;
import org.zeroturnaround.javarebel.IntegrationFactory;
import org.zeroturnaround.javarebel.integration.generic.FindResourceClassResourceSource;

/**
 *
 */
public class JRebelLayerListener implements LayerListener {

    public void onModuleLoaded(Module module) {
        Integration instance = IntegrationFactory.getInstance();
        instance.registerClassLoader(module.getClassLoader(), new FindResourceClassResourceSource(module.getClassLoader()));
    }
}
