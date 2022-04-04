package io.rhizomatic.kernel.layer;

import io.rhizomatic.api.Monitor;
import io.rhizomatic.api.RhizomaticException;
import io.rhizomatic.api.layer.RzLayer;
import io.rhizomatic.api.layer.RzModule;
import io.rhizomatic.kernel.spi.layer.LayerListener;
import io.rhizomatic.kernel.spi.layer.LoadedLayer;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import static io.rhizomatic.kernel.layer.LayerSorter.topologicalSort;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Loads JPMS layers into the system. A topological sort is performed on the layers so that they are loaded parent-first.
 *
 * Layers may form a network with multiple parents. Each layer has one classloader, which is the parent of all contained module classloaders. The layer classloader may have
 * N parents corresponding to the parent laeyr's classloaders. Consequently, modules in the same layer will share the same parent classloader and have visibility to parent
 * classloaders.
 */
public class LayerManager {
    private Monitor monitor;

    public LayerManager(Monitor monitor) {
        this.monitor = monitor;
    }

    /**
     * Loads the layers into the system.
     */
    public List<LoadedLayer> load(List<RzLayer> layers, Set<String> openToNames) {

        var openToModules = new HashSet<Module>();
        for (var name : openToNames) {
            var module = ModuleLayer.boot().findModule(name).orElseThrow(() -> new RhizomaticException("Module not found: " + name));
            openToModules.add(module);
        }

        var listeners = ServiceLoader.load(LayerListener.class).stream().map(ServiceLoader.Provider::get).collect(toSet());

        listeners.forEach(l -> l.onLayerConfiguration(layers, monitor));

        var sortedLayers = topologicalSort(layers);

        var mappings = new HashMap<RzLayer, LayerMapping>();

        for (var layer : sortedLayers) {
            loadLayer(layer, mappings, openToModules, listeners);
        }
        return mappings.values().stream().map(LayerMapping::toLoadedLayer).collect(toList());
    }

    /**
     * Releases open resources.
     */
    public void release() {

    }

    void loadLayer(RzLayer layer, Map<RzLayer, LayerMapping> mappings, Set<Module> openModules, Set<LayerListener> listeners) {
        var bootLayer = ModuleLayer.boot();

        // load the layer and its modules in a single classloader
        var parentLayers = new ArrayList<ModuleLayer>();
        for (var parentLayer : layer.getParents()) {
            var mapping = mappings.get(parentLayer);
            if (mapping == null) {
                throw new RuntimeException("Parent layer mapping not found: " + parentLayer.getName());
            }
            parentLayers.add(mapping.controller.layer());
        }
        if (parentLayers.isEmpty()) {
            // boot layer is the parent if this layer has none specified
            parentLayers.add(bootLayer);
        }

        // Each layer has a networked classloader that delegates to parent layers; each contained module has its own classloader which delegates to the layer networked classloader
        var layerLoader = createLayerClassLoader(layer, mappings);

        var modulePaths = layer.getModules().stream().map(RzModule::getLocation).toArray(Path[]::new);
        var finder = ModuleFinder.of(modulePaths);

        // all modules are root modules
        var moduleNames = finder.findAll().stream().map(mr -> mr.descriptor().name()).collect(toSet());

        var configuration = Configuration.resolve(finder, List.of(bootLayer.configuration()), ModuleFinder.of(), moduleNames);

        var controller = ModuleLayer.defineModulesWithManyLoaders(configuration, parentLayers, layerLoader);

        listeners.forEach(l -> l.onLayerLoaded(controller, monitor));

        // open the layer modules to subsystem modules
        for (var module : controller.layer().modules()) {
            for (var targetModule : openModules) {
                for (var pkg : module.getPackages()) {
                    controller.addOpens(module, pkg, targetModule);
                }
            }
            listeners.forEach(l -> l.onModuleLoaded(module,  monitor));
        }

        var moduleReferences = finder.findAll();
        var mapping = new LayerMapping(controller, layerLoader, moduleReferences);
        mappings.put(layer, mapping);
    }

    /**
     * Creates a classloader for the layer, assigning the parent layers' classloaders.
     */
    private ClassLoader createLayerClassLoader(RzLayer layer, Map<RzLayer, LayerMapping> mappings) {
        var systemLoader = ClassLoader.getSystemClassLoader();

        var parentLoaders = getParentLoaders(layer, mappings);

        return new NetworkedClassLoader(layer.getName(), systemLoader, parentLoaders);
    }

    /**
     * Returns the parent layers' classloaders.
     */
    private Set<ClassLoader> getParentLoaders(RzLayer layer, Map<RzLayer, LayerMapping> mappings) {
        var parentLoaders = new LinkedHashSet<ClassLoader>();
        for (var parentLayer : layer.getParents()) {
            var parentLoader = mappings.get(parentLayer).classLoader;
            if (parentLoader == null) {
                throw new RuntimeException("Classloader not found for parent layer: " + parentLayer.getName());
            }
            parentLoaders.add(parentLoader);
        }
        return parentLoaders;
    }


    private static class LayerMapping {
        ModuleLayer.Controller controller;
        Set<ModuleReference> moduleReferences;
        ClassLoader classLoader;

        public LayerMapping(ModuleLayer.Controller controller, ClassLoader classLoader, Set<ModuleReference> moduleReferences) {
            this.controller = controller;
            this.classLoader = classLoader;
            this.moduleReferences = moduleReferences;
        }

        public LoadedLayer toLoadedLayer() {
            return new LoadedLayer(controller.layer(), classLoader, moduleReferences);
        }
    }

}
