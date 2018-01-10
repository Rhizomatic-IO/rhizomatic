package io.rhizomatic.api.layer;

import io.rhizomatic.api.internal.PathUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Paths;

/**
 *
 */
public class UnixPathUtilsTest {

    @Test
    public void verifyToSegments() {
        String[] segments = PathUtils.toSegments(Paths.get("foo/bar/baz"));
        Assert.assertEquals("foo", segments[0]);
        Assert.assertEquals("bar", segments[1]);
        Assert.assertEquals("baz", segments[2]);
    }

    @Before
    public void windowsOnly() {
        Assume.assumeTrue(!System.getProperty("os.name").startsWith("Windows"));
    }
}