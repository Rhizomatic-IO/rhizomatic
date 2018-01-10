package io.rhizomatic.kernel.parse;

import org.junit.Assert;
import org.junit.Test;

import static junit.framework.TestCase.fail;

/**
 *
 */
public class ArgsParserTest {

    @Test
    public void parseParams() {
        ArgsParser.Params params = ArgsParser.parseParams("-c", "c1");
        Assert.assertEquals("c1", params.configPath);

        params = ArgsParser.parseParams("-l", "l1");
        Assert.assertEquals("l1", params.layersPath);

        params = ArgsParser.parseParams("-m", "m1");
        Assert.assertEquals("m1", params.modulePath);

        params = ArgsParser.parseParams("-c", "c1", "-l", "l1");
        Assert.assertEquals("c1", params.configPath);
        Assert.assertEquals("l1", params.layersPath);

        try {
            ArgsParser.parseParams("-c", "c1", "-l", "l1", "-m", "m1");
            fail("Module and layer location exclusivity failed");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            ArgsParser.parseParams("-f", "f");
            fail("Invalid flag");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            ArgsParser.parseParams("-l", "l1", "-c");
            fail("Invalid number of arguments");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
}