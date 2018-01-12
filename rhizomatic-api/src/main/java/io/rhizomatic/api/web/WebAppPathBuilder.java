package io.rhizomatic.api.web;

import io.rhizomatic.api.RhizomaticException;
import io.rhizomatic.api.internal.PathLayoutVisitor;

import java.io.IOException;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static io.rhizomatic.api.internal.PathUtils.walkParents;

/**
 * Builds a web application definition based on a root path. The included webapp locations can be customized based on inclusions, exclusions and Java class file directory partial
 * paths.
 *
 * This builder can be used to a create webapp definition from the file system image of a soource code repository.
 */
public class WebAppPathBuilder {
    private String contextPath;
    private Path root;
    private PathLayoutVisitor.Builder visitorBuilder;

    /**
     * Returns a new builder.
     */
    public static WebAppPathBuilder newInstance() {
        return new WebAppPathBuilder();
    }

    /**
     * Sets the web context path, i.e. the context part of the web app URL.
     */
    public WebAppPathBuilder contextPath(String contextPath) {
        this.contextPath = contextPath;
        return this;
    }

    /**
     * Sets the root path to start traversal from.
     */
    public WebAppPathBuilder root(Path root) {
        this.root = root;
        return this;
    }

    /**
     * Finds the root path to start traversal from by walking the path hierarchy from the current context location until the predicate is satisfied.
     */
    public WebAppPathBuilder findRoot(Predicate<Path> predicate) {
        this.root = walkParents(predicate);
        return this;
    }

    /**
     * The path fragment that contains Java classes contained in modules, e.g. project/out/production/classes or project/build/classes. Separators will be converted to
     * their platform-specific variant.
     */
    public WebAppPathBuilder contentRootPath(String path) {
        visitorBuilder.matchPath(path);
        return this;
    }

    public WebAppPathBuilder includes(String... paths) {
        visitorBuilder.includes(paths);
        return this;
    }

    public WebAppPathBuilder excludes(String... paths) {
        visitorBuilder.includes(paths);
        return this;
    }

    public WebApp build() {
        Objects.requireNonNull(contextPath, "Web context path cannot be null");
        Objects.requireNonNull(root, "Root path cannot be null");
        try {
            List<Path> paths = new ArrayList<>();
            visitorBuilder.callback(paths::add);
            FileVisitor<Path> visitor = visitorBuilder.build();
            Files.walkFileTree(root, visitor);
            return new WebApp(contextPath, paths.toArray(new Path[paths.size()]));
        } catch (IOException e) {
            throw new RhizomaticException(e);
        }
    }

    private WebAppPathBuilder() {
        visitorBuilder = PathLayoutVisitor.Builder.newInstance();
    }

}
