package io.rhizomatic.reload.jrebel;

import io.rhizomatic.api.Monitor;
import io.rhizomatic.kernel.spi.layer.LoadedLayer;
import io.rhizomatic.kernel.spi.reload.RzReloader;
import io.rhizomatic.kernel.spi.subsystem.Subsystem;
import io.rhizomatic.kernel.spi.subsystem.SubsystemContext;
import io.rhizomatic.kernel.spi.util.ClassHelper;
import org.zeroturnaround.javarebel.ReloaderFactory;
import org.zeroturnaround.javarebel.integration.generic.ClassEventListenerAdapter;

import java.io.IOException;
import java.lang.module.ModuleReference;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Enables a system with JRebel class reloading.
 */
public class JRebelSubsystem extends Subsystem {
    private Monitor monitor;
    private WatchService watchService;
    private ExecutorService executorService;
    private RzReloader reloader;

    public JRebelSubsystem() {
        super("rhizomatic.jrebel");
    }

    public void assemble(SubsystemContext context) {
        monitor = context.getMonitor();
        reloader = context.resolve(RzReloader.class);
    }

    public void start(SubsystemContext context) {
        try {
            watchService = FileSystems.getDefault().newWatchService();

            Map<WatchKey, Holder> modules = new HashMap<>();
            for (LoadedLayer loadedLayer : context.getLoadedLayers()) {
                for (ModuleReference mr : loadedLayer.getReferences()) {
                    if (!mr.location().isPresent()) {
                        return;
                    }
                    Path moduleRoot = Paths.get(mr.location().get());
                    if (!Files.isDirectory(moduleRoot)) {
                        return;
                    }
                    Module module = loadedLayer.getModule(mr);
                    FileVisitor<Path> visitor = new SimpleFileVisitor<>() {
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                            WatchKey key = dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
                            modules.put(key, new Holder(module, moduleRoot));
                            return FileVisitResult.CONTINUE;
                        }
                    };
                    Files.walkFileTree(moduleRoot, visitor);

                }
            }
            if (!modules.isEmpty()) {
                executorService = Executors.newSingleThreadExecutor();
                executorService.submit(() -> {
                    while (true) {
                        WatchKey key = watchService.take();
                        List<WatchEvent<?>> events = key.pollEvents();
                        for (WatchEvent<?> event : events) {
                            if (!(event.context() instanceof Path)) {
                                continue;
                            }
                            Path changed = (Path) event.context();
                            if (changed.getFileName().toString().endsWith(".class")) {
                                // find the module the path is located in
                                Holder holder = modules.get(key);
                                if (holder == null) {
                                    continue;
                                }

                                Path changedFullPath = ((Path) key.watchable()).resolve(changed);
                                String relativeName = changedFullPath.toString().substring(holder.moduleRoot.toString().length() + 1);
                                Class<?> addedClass = holder.module.getClassLoader().loadClass(ClassHelper.getClassName(relativeName));
                                monitor.info(() -> "Class added: " + addedClass.getName());
                                reloader.classAdded(addedClass);
                            }

                        }
                        key.reset();
                    }
                });

            }
            ReloaderFactory.getInstance().addClassReloadListener(new ClassEventListenerAdapter(0) {
                public void onClassEvent(int eventType, Class<?> clazz) {
                    context.getMonitor().info(() -> "Reload captured: " + clazz.getName());
                    reloader.classChanged(clazz);
                }
            });

        } catch (IOException e) {
            monitor.severe(() -> "Error initializing reloader subsystem. Reloading will be disabled.", e);
        }

    }

    public void shutdown() {
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                monitor.severe(() -> "Error shutting down reloader subsystem", e);
            }
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    private class Holder {
        Module module;
        Path moduleRoot;

        public Holder(Module module, Path moduleRoot) {
            this.module = module;
            this.moduleRoot = moduleRoot;
        }
    }
}
