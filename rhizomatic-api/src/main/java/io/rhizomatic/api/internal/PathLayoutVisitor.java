package io.rhizomatic.api.internal;

import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Visits a file system layout, matching paths based on the configured path segments, inclusions, and exclusions.
 */
public class PathLayoutVisitor extends SimpleFileVisitor<Path> {
    private static final String[][] EMPTY = new String[0][];
    private List<String[]> matchPaths = new ArrayList<>();
    private String[][] includes = EMPTY;
    private String[][] excludes = EMPTY;

    private Consumer<Path> callback = (path) -> {
    };

    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        if (dir.getFileName().toString().startsWith(".")) {
            return FileVisitResult.SKIP_SUBTREE;
        }
        String[] segments = PathUtils.toSegments(dir);
        if (excludes != EMPTY) {
            for (String[] exclude : excludes) {
                if (segments.length < exclude.length) {
                    continue;
                }
                if (PathUtils.indexOf(segments, exclude) >= 0) {
                    // exclude directory and its children
                    return FileVisitResult.SKIP_SUBTREE;
                }
            }
        }
        if (includes != EMPTY) {
            boolean match = false;
            for (String[] include : includes) {
                if (segments.length < include.length) {
                    continue;
                }
                if (PathUtils.indexOf(segments, include) >= 0) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                // no match
                return FileVisitResult.CONTINUE;
            }
        }
        boolean match = false;
        for (String[] matchSegments : matchPaths) {
            if (PathUtils.endsWith(segments, matchSegments)) {
                // match
                match = true;
                callback.accept(dir);
            }
        }
        return match ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
    }

    public static class Builder {
        private PathLayoutVisitor visitor;

        public static Builder newInstance() {
            return new Builder();
        }

        /**
         * Path segment to match, e.g. foo/bar/bax will match against the segment foo/bar.
         */
        public Builder matchPath(String matchPath) {
            Objects.requireNonNull(matchPath, "Path was null");
            if (matchPath.trim().length() == 0) {
                throw new IllegalArgumentException("Path was empty");
            }
            String[] segements = matchPath.split("/");
            visitor.matchPaths.add(segements);
            return this;
        }

        /**
         * Include the given path fragments.
         */
        public Builder includes(String... paths) {
            Objects.requireNonNull(paths, "Include paths was null");
            visitor.includes = new String[paths.length][];
            for (int i = 0; i < paths.length; i++) {
                visitor.includes[i] = paths[i].split("/");

            }
            return this;
        }

        /**
         * Exclude the given path fragments.
         */
        public Builder excludes(String... paths) {
            Objects.requireNonNull(paths, "Exclude paths was null");
            visitor.excludes = new String[paths.length][];
            for (int i = 0; i < paths.length; i++) {
                visitor.excludes[i] = paths[i].split("/");
            }
            return this;
        }

        /**
         * Callback to invoke when a path is matched.
         */
        public Builder callback(Consumer<Path> callback) {
            visitor.callback = callback;
            return this;
        }

        public PathLayoutVisitor build() {
            return visitor;
        }

        private Builder() {
            visitor = new PathLayoutVisitor();
        }

    }

}
