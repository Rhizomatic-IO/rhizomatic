package io.rhizomatic.api.internal;

import io.rhizomatic.api.RhizomaticException;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * Utilities for building layer configurations.
 */
public class PathUtils {

    /**
     * Converts a path to string segments based on the platform file separator.
     */
    public static String[] toSegments(Path path) {
        var segments = new ArrayList<String>();
        for (var i = 0; i < path.getNameCount(); i++) {
            segments.add(path.getName(i).toString());
        }
        return segments.toArray(new String[0]);
    }

    /**
     * Returns the starting index of the fragment in the array or -1 if not found.
     */
    public static int indexOf(String[] array, String[] fragment) {
        requireNonNull(array, "array");
        requireNonNull(fragment, "target");
        if (fragment.length == 0) {
            return 0;
        }

        outer:
        for (var i = 0; i < array.length - fragment.length + 1; i++) {
            for (int j = 0; j < fragment.length; j++) {
                if (!array[i + j].equals(fragment[j])) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    /**
     * Returns true if the array ends with the fragment.
     */
    public static boolean endsWith(String[] array, String[] fragment) {
        int pos = indexOf(array, fragment);
        return pos == array.length - fragment.length;
    }

    public static Path walkParents(Predicate<Path> predicate) {
        try {
            var anchor = Paths.get(predicate.getClass().getResource("").toURI());
            anchor = anchor.normalize();
            while (anchor.getParent() != null) {
                anchor = anchor.getParent();
                if (predicate.test(anchor)) {
                    return anchor;
                }
            }
        } catch (URISyntaxException e) {
            throw new RhizomaticException("Unable top determine path location", e);
        }
        throw new RhizomaticException("Path location not found");

    }
}
