package io.rhizomatic.api.layer;

import io.rhizomatic.api.internal.PathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

/**
 *
 */
public class UnixPathUtilsTest {

    @Test
    public void verifyToSegments() {
        String[] segments = PathUtils.toSegments(Paths.get("foo/bar/baz"));
        Assertions.assertEquals("foo", segments[0]);
        Assertions.assertEquals("bar", segments[1]);
        Assertions.assertEquals("baz", segments[2]);
    }

    @BeforeEach
    public void windowsOnly() {
        Assumptions.assumeTrue(!System.getProperty("os.name").startsWith("Windows"));
    }
}