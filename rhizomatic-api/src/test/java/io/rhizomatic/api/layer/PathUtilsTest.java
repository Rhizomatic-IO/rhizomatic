package io.rhizomatic.api.layer;

import io.rhizomatic.api.internal.PathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class PathUtilsTest {

    @Test
    public void verifyIndexOf() {
        Assertions.assertTrue(PathUtils.indexOf("foo/bar/baz/quux".split("/"), "bar/baz".split("/")) > 0);
        Assertions.assertTrue(PathUtils.indexOf("foo/bar/baz/quux".split("/"), "foo/bar/baz".split("/")) == 0);
        Assertions.assertTrue(PathUtils.indexOf("foo/bar/baz".split("/"), "foo/bar/baz".split("/")) == 0);
        Assertions.assertTrue(PathUtils.indexOf("foo1/bar/baz".split("/"), "foo/bar".split("/")) == -1);
        Assertions.assertTrue(PathUtils.indexOf("foo1/bar/baz".split("/"), "".split("/")) == -1);
    }

    @Test
    public void verifyEndsWith() {
        Assertions.assertTrue(PathUtils.endsWith("foo/bar/baz".split("/"), "bar/baz".split("/")));
        Assertions.assertFalse(PathUtils.endsWith("foo/bar".split("/"), "bar/baz".split("/")));
        Assertions.assertFalse(PathUtils.endsWith("foo/bar".split("/"), "foo/baz".split("/")));
        Assertions.assertFalse(PathUtils.endsWith("foo/bar".split("/"), "foo".split("/")));
    }
}