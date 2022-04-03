package io.rhizomatic.api.layer;

import io.rhizomatic.api.RhizomaticException;
import io.rhizomatic.api.internal.PathLayoutVisitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

import static io.rhizomatic.api.internal.PathUtils.walkParents;
import static java.util.Objects.requireNonNull;

/**
 * Builds a layer definition based on a root path. The included module locations can be customized based on inclusions, exclusions and Java class file directory partial paths.
 *
 * This builder can be used to create layer definition from the file system image of a soource code repository.
 */
public class LayerPathBuilder {
    private String name;
    private Path root;

    private PathLayoutVisitor.Builder visitorBuilder;

    /**
     * Returns a new builder.
     *
     * @param name the layer name.
     */
    public static LayerPathBuilder newInstance(String name) {
        return new LayerPathBuilder(name);
    }

    /**
     * Sets the root path to start traversal from.
     */
    public LayerPathBuilder root(Path root) {
        this.root = root;
        return this;
    }

    /**
     * Finds the root path to start traversal from by walking the path hierarchy from the current context location until the predicate is satisfied.
     */
    public LayerPathBuilder findRoot(Predicate<Path> predicate) {
        this.root = walkParents(predicate);
        return this;
    }

    /**
     * The path fragment that contains Java classes contained in modules, e.g. project/out/production/classes or project/build/classes. Separators will be converted to
     * their platform-specific variant.
     */
    public LayerPathBuilder javaPath(String path) {
        requireNonNull(path, "Java path was null");
        if (path.trim().length() == 0) {
            throw new IllegalArgumentException("Java path was empty");
        }
        visitorBuilder.matchPath(path);
        return this;
    }

    /**
     * Sets path fragments to include.
     */
    public LayerPathBuilder includes(String... paths) {
        visitorBuilder.includes(paths);
        return this;
    }

    /**
     * Sets path fragments to exclude.
     */
    public LayerPathBuilder excludes(String... paths) {
        visitorBuilder.excludes(paths);
        return this;
    }


    public RzLayer build() {
        requireNonNull(root, "Root path not set");
        try {
            var layerBuilder = RzLayer.Builder.newInstance(name);
            visitorBuilder.callback(layerBuilder::module);

            var visitor = visitorBuilder.build();

            Files.walkFileTree(root, visitor);
            return layerBuilder.build();
        } catch (IOException e) {
            throw new RhizomaticException(e);
        }
    }

    private LayerPathBuilder(String name) {
        this.name = name;
        visitorBuilder = PathLayoutVisitor.Builder.newInstance();
    }

}
