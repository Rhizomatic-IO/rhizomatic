package io.rhizomatic.kernel.layer;

import io.rhizomatic.api.layer.RzLayer;
import io.rhizomatic.kernel.graph.DirectedGraph;
import io.rhizomatic.kernel.graph.TopologicalSorter;
import io.rhizomatic.kernel.graph.Vertex;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

/**
 * Topologically sorts of a collection of layers.
 */
public class LayerSorter {

    /**
     * Sorts the layers topologically.
     */
    public static List<RzLayer> topologicalSort(List<RzLayer> layers) {
        var graph = new DirectedGraph<RzLayer>();
        var seen = new HashSet<RzLayer>();

        for (var layer : layers) {
            if (seen.contains(layer)) {
                continue;
            }
            seen.add(layer);
            var parentVertex = new Vertex<RzLayer>(layer);
            graph.add(parentVertex);
            buildGraph(parentVertex, graph, seen);
        }
        var sorter = new TopologicalSorter<RzLayer>();
        return sorter.sort(graph).stream().map(Vertex::getEntity).collect(toList());
    }

    private static void buildGraph(Vertex<RzLayer> parentVertex, DirectedGraph<RzLayer> graph, Set<RzLayer> seen) {
        for (var child : parentVertex.getEntity().getChildren()) {
            if (seen.contains(child)) {
                continue;
            }
            seen.add(child);
            var childVertex = new Vertex<>(child);
            graph.add(childVertex);
            graph.addEdge(parentVertex, childVertex);
            buildGraph(childVertex, graph, seen);
        }
    }

    private LayerSorter() {
    }
}
