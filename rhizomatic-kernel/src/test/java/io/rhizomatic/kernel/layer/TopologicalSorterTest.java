package io.rhizomatic.kernel.layer;

import io.rhizomatic.api.layer.RzLayer;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 *
 */
public class TopologicalSorterTest {

    @Test
    public void verifyTopologicalSort() {
        // Layer 4 has three parents: 1,2,3. Verifies that the ordering is 1-->2,3-->4 since 4 must come after all of its parents
        LayerManager layerManager = new LayerManager();

        RzLayer layer1 = RzLayer.Builder.newInstance("layer1").build();
        RzLayer layer2 = RzLayer.Builder.newInstance("layer2").parent(layer1).build();
        RzLayer layer3 = RzLayer.Builder.newInstance("layer3").parent(layer1).build();

        RzLayer layer4 = RzLayer.Builder.newInstance("layer4").parent(layer2).parent(layer3).parent(layer1).build();

        List<RzLayer> sorted = TopologicalSorter.topologicalSort(List.of(layer1, layer4, layer3, layer2));

        Assert.assertSame(layer1, sorted.get(0));  // first must be layer 1
        Assert.assertSame(layer4, sorted.get(3));  // last must be layer 4
    }

}