package io.rhizomatic.kernel.layer;

import io.rhizomatic.api.RhizomaticException;

import java.lang.module.ModuleReference;
import java.util.Set;

/**
 * A JPMS layer loaded in the system.
 */
public class LoadedLayer {
    private ModuleLayer moduleLayer;
    private ClassLoader classLoader;
    private Set<ModuleReference> references;

    public LoadedLayer(ModuleLayer moduleLayer, ClassLoader classLoader, Set<ModuleReference> references) {
        this.moduleLayer = moduleLayer;
        this.classLoader = classLoader;
        this.references = references;
    }

    /**
     * Returns the underlying JPMS layer.
     */
    public ModuleLayer getModuleLayer() {
        return moduleLayer;
    }

    /**
     * The classloader associated with the layer. This classloader is the parent of all contained module classloaders.
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Contained module references.
     */
    public Set<ModuleReference> getReferences() {
        return references;
    }

    /**
     * Returns the module for the reference contained in the current layer.
     */
    public Module getModule(ModuleReference reference) {
        String name = reference.descriptor().name();
        return moduleLayer.findModule(name).orElseThrow(() -> new RhizomaticException("Module not found: " + name));
    }
}
