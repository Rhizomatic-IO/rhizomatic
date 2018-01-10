package io.rhizomatic.api.layer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A JPMS layer configuration.
 */
public class RzLayer {
    private String name;

    private List<RzLayer> parents = new ArrayList<>();
    private List<RzLayer> children = new ArrayList<>();

    private List<RzModule> modules = new ArrayList<>();

    public String getName() {
        return name;
    }

    public List<RzLayer> getParents() {
        return parents;
    }

    public List<RzLayer> getChildren() {
        return children;
    }

    public List<RzModule> getModules() {
        return modules;
    }

    private RzLayer(String name) {
        this.name = name;
    }

    public static class Builder {
        private RzLayer layer;

        public static Builder newInstance(String name) {
            return new Builder(name);
        }

        public Builder parent(RzLayer parentLayer) {
            layer.parents.add(parentLayer);
            parentLayer.children.add(layer);
            return this;
        }

        public Builder module(RzModule module) {
            layer.modules.add(module);
            module.setParent(layer);
            return this;
        }

        public Builder module(Path location) {
            RzModule module = new RzModule(location);
            return module(module);
        }

        public RzLayer build() {
            return layer;
        }

        private Builder(String name) {
            layer = new RzLayer(name);
        }

    }
}
