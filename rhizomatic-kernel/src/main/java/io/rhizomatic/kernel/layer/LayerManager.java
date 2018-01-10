package io.rhizomatic.kernel.layer;

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

import static io.rhizomatic.kernel.layer.TopologicalSorter.topologicalSort;
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

    /**
     * Loads the layers into the system.
     */
    public List<LoadedLayer> load(List<RzLayer> layers, Set<String> openToNames) {

        Set<Module> openToModules = new HashSet<>();
        for (String name : openToNames) {
            Module module = ModuleLayer.boot().findModule(name).orElseThrow(() -> new RhizomaticException("Module not found: " + name));
            openToModules.add(module);
        }

        Set<LayerListener> listeners = ServiceLoader.load(LayerListener.class).stream().map(ServiceLoader.Provider::get).collect(toSet());

        listeners.forEach(l -> l.onLayerConfiguration(layers));

        List<RzLayer> sortedLayers = topologicalSort(layers);

        Map<RzLayer, LayerMapping> mappings = new HashMap<>();

        for (RzLayer layer : sortedLayers) {
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
        ModuleLayer bootLayer = ModuleLayer.boot();

        // load the layer and its modules in a single classloader
        List<ModuleLayer> parentLayers = new ArrayList<>();
        for (RzLayer parentLayer : layer.getParents()) {
            LayerMapping mapping = mappings.get(parentLayer);
            if (mapping == null) {
                throw new RuntimeException("Parent layer mapping not found: " + parentLayer.getName());
            }
            parentLayers.add(mapping.controller.layer());
        }
        if (parentLayers.isEmpty()) {
            // boot layer is the parent if this layer has none specified
            parentLayers.add(bootLayer);
        }

        // Each layer has a networked classloader that delegates to parent layers; each contained module has its own classloader whoch delegates to the layer networked classloader
        ClassLoader layerLoader = createLayerClassLoader(layer, mappings);

        Path[] modulePaths = layer.getModules().stream().map(RzModule::getLocation).toArray(Path[]::new);
        ModuleFinder finder = ModuleFinder.of(modulePaths);

        // all modules are root modules
        Set<String> moduleNames = finder.findAll().stream().map(mr -> mr.descriptor().name()).collect(toSet());

        Configuration configuration = Configuration.resolve(finder, List.of(bootLayer.configuration()), ModuleFinder.of(), moduleNames);

        ModuleLayer.Controller controller = ModuleLayer.defineModulesWithManyLoaders(configuration, parentLayers, layerLoader);

        listeners.forEach(l -> l.onLayerLoaded(controller));

        // open the layer modules to subsystem modules
        for (Module module : controller.layer().modules()) {
            for (Module targetModule : openModules) {
                for (String pkg : module.getPackages()) {
                    controller.addOpens(module, pkg, targetModule);
                }
            }
            listeners.forEach(l -> l.onModuleLoaded(module));
        }

        Set<ModuleReference> moduleReferences = finder.findAll();
        LayerMapping mapping = new LayerMapping(controller, layerLoader, moduleReferences);
        mappings.put(layer, mapping);
    }

    /**
     * Creates a classloader for the layer, assigning the parent layers' classloaders.
     */
    private ClassLoader createLayerClassLoader(RzLayer layer, Map<RzLayer, LayerMapping> mappings) {
        ClassLoader systemLoader = ClassLoader.getSystemClassLoader();

        Set<ClassLoader> parentLoaders = getParentLoaders(layer, mappings);

        return new NetworkedClassLoader(layer.getName(), systemLoader, parentLoaders);
    }

    /**
     * Returns the parent layers' classloaders.
     */
    private Set<ClassLoader> getParentLoaders(RzLayer layer, Map<RzLayer, LayerMapping> mappings) {
        Set<ClassLoader> parentLoaders = new LinkedHashSet<>();
        for (RzLayer parentLayer : layer.getParents()) {
            ClassLoader parentLoader = mappings.get(parentLayer).classLoader;
            if (parentLoader == null) {
                throw new RuntimeException("Classloader not found for parent layer: " + parentLayer.getName());
            }
            parentLoaders.add(parentLoader);
        }
        return parentLoaders;
    }


    private class LayerMapping {
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
