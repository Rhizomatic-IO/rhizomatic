package io.rhizomatic.kernel.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Implements depth first search.
 */
public class DepthFirstTraversal {

    public static <T> List<Vertex<T>> traverse(DirectedGraph<T> graph, Vertex<T> start) {
        return traverse(graph, start, (v) -> true);
    }

    private static <T> List<Vertex<T>> traverse(DirectedGraph<T> graph, Vertex<T> start, Predicate<Vertex> visitor) {
        List<Vertex<T>> visited = new ArrayList<>();
        List<Vertex<T>> stack = new ArrayList<>();
        Set<Vertex<T>> seen = new HashSet<>(visited);
        stack.add(start);
        seen.add(start);
        do {
            // mark as visited
            Vertex<T> next = stack.remove(stack.size() - 1);
            visited.add(next);
            if (!visitor.test(next)) {
                return visited;
            }

            // add all non-visited adjacent vertices to the stack
            Set<Vertex<T>> adjacentVertices = graph.getAdjacentVertices(next);
            for (Vertex<T> v : adjacentVertices) {
                seen.add(v);
                stack.add(v);
            }

        } while (!stack.isEmpty());
        return visited;
    }


}
