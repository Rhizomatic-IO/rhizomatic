package io.rhizomatic.api.layer;

import org.junit.Assert;
import org.junit.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 */
public class LayerPathBuilderTest {

    @Test
    public void verifyLayerIsBuiltWithOneModule() throws URISyntaxException {
        LayerPathBuilder builder = LayerPathBuilder.newInstance("main");

        Path dir = getRootCompilePath();

        builder.root(dir);  // start at root compilation path

        //the Java path will also be the root compilation path
        builder.javaPath(dir.getFileName().toString());
        
        RzLayer layer = builder.build();

        // verify module location is the root compilation path of this class
        Assert.assertEquals(dir, layer.getModules().get(0).getLocation());
    }

    private Path getRootCompilePath() throws URISyntaxException {
        Path dir = Paths.get(getClass().getResource(".").toURI());
        int count = getClass().getPackageName().split("\\.").length;
        for (int i = 0; i < count; i++) {
            dir = dir.getParent();
        }
        return dir;
    }
}