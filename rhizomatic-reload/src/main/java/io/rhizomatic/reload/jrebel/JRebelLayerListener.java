package io.rhizomatic.reload.jrebel;

import io.rhizomatic.api.Monitor;
import io.rhizomatic.kernel.spi.layer.LayerListener;
import org.zeroturnaround.javarebel.ReloaderFactory;
import org.zeroturnaround.javarebel.integration.generic.ClassEventListenerAdapter;

/**
 * Captures class reload events from JRebel.
 */
public class JRebelLayerListener implements LayerListener {

    public void onLayerLoaded(ModuleLayer.Controller controller, Monitor monitor) {
        ReloaderFactory.getInstance().addClassReloadListener(new ClassEventListenerAdapter(0) {
            public void onClassEvent(int eventType, Class<?> clazz) {
                monitor.info(() -> "Reload captured: " + clazz.getName());
            }
        });
    }

}
