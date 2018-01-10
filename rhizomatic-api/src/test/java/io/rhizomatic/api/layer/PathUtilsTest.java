package io.rhizomatic.api.layer;

import io.rhizomatic.api.internal.PathUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class PathUtilsTest {

    @Test
    public void verifyIndexOf() {
        Assert.assertTrue(PathUtils.indexOf("foo/bar/baz/quux".split("/"), "bar/baz".split("/")) > 0);
        Assert.assertTrue(PathUtils.indexOf("foo/bar/baz/quux".split("/"), "foo/bar/baz".split("/")) == 0);
        Assert.assertTrue(PathUtils.indexOf("foo/bar/baz".split("/"), "foo/bar/baz".split("/")) == 0);
        Assert.assertTrue(PathUtils.indexOf("foo1/bar/baz".split("/"), "foo/bar".split("/")) == -1);
        Assert.assertTrue(PathUtils.indexOf("foo1/bar/baz".split("/"), "".split("/")) == -1);
    }

    @Test
    public void verifyEndsWith() {
        Assert.assertTrue(PathUtils.endsWith("foo/bar/baz".split("/"), "bar/baz".split("/")));
        Assert.assertFalse(PathUtils.endsWith("foo/bar".split("/"), "bar/baz".split("/")));
        Assert.assertFalse(PathUtils.endsWith("foo/bar".split("/"), "foo/baz".split("/")));
        Assert.assertFalse(PathUtils.endsWith("foo/bar".split("/"), "foo".split("/")));
    }
}