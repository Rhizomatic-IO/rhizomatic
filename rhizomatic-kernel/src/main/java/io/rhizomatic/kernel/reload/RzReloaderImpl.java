package io.rhizomatic.kernel.reload;

import io.rhizomatic.kernel.scan.ClassScanner;
import io.rhizomatic.kernel.spi.inject.InstanceManager;
import io.rhizomatic.kernel.spi.reload.ReloadListener;
import io.rhizomatic.kernel.spi.reload.RzReloader;
import io.rhizomatic.kernel.spi.scan.ScanIndex;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class RzReloaderImpl implements RzReloader {
    private InstanceManager instanceManager;
    private ClassScanner scanner;
    private List<ReloadListener> listeners = new ArrayList<>();

    public void classChanged(Class<?> clazz) {
        // TODO diff the injection points for singletons and inject manually
        Object instance = instanceManager.resolve(clazz);
        listeners.forEach(l -> l.onInstanceChanged(instance));
    }

    public void classAdded(Class<?> clazz) {
        ScanIndex index = scanner.scan(Set.of(clazz));
        if (index.getServiceBindings().isEmpty()) {
            return;  // the class is not a service, ignore
        }
        instanceManager.wire(index);
        Object instance = instanceManager.resolve(clazz);
        listeners.forEach(l -> l.onInstanceAdded(instance));
    }

    public void register(ReloadListener listener) {
        listeners.add(listener);
    }

    public void initialize(InstanceManager instanceManager, ClassScanner scanner) {
        this.instanceManager = instanceManager;
        this.scanner = scanner;
    }
}
