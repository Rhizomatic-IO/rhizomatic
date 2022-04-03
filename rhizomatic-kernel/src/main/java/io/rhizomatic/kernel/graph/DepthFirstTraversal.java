package io.rhizomatic.kernel.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Implements depth first search.
 */
public class DepthFirstTraversal {

    public static <T> List<Vertex<T>> traverse(DirectedGraph<T> graph, Vertex<T> start) {
        return traverse(graph, start, (v) -> true);
    }

    private static <T> List<Vertex<T>> traverse(DirectedGraph<T> graph, Vertex<T> start, Predicate<Vertex<T>> visitor) {
        var visited = new ArrayList<Vertex<T>>();
        var stack = new ArrayList<Vertex<T>>();
        stack.add(start);
        do {
            // mark as visited
            var next = stack.remove(stack.size() - 1);
            visited.add(next);
            if (!visitor.test(next)) {
                return visited;
            }

            // add all non-visited adjacent vertices to the stack
            var adjacentVertices = graph.getAdjacentVertices(next);
            stack.addAll(adjacentVertices);

        } while (!stack.isEmpty());
        return visited;
    }


}
