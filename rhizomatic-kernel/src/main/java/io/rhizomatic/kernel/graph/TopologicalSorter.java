package io.rhizomatic.kernel.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default implementation of a topological sorter.
 */
public class TopologicalSorter<T> {

    /**
     * Performs a topological sort of the graph.
     */
    public List<Vertex<T>> sort(DirectedGraph<T> dag) throws CycleException {
        // perform the sort over the entire graph, calculating roots and references for all children
        Map<Vertex<T>, AtomicInteger> vertexMap = new HashMap<>();
        List<Vertex<T>> roots = new ArrayList<>();
        // first pass over the graph to collect vertex references and root vertices
        Set<Vertex<T>> vertices = dag.getVertices();
        for (Vertex<T> v : vertices) {
            int incoming = dag.getIncomingEdges(v).size();
            if (incoming == 0) {
                roots.add(v);
            } else {
                AtomicInteger count = new AtomicInteger();
                count.set(incoming);
                vertexMap.put(v, count);
            }
        }
        // perform the sort
        return sort(dag, vertexMap, roots);
    }


    /**
     * Performs a topological sort of the subgraph reachable from the outgoing edges of the given vertex.
     */
    public List<Vertex<T>> sort(DirectedGraph<T> dag, Vertex<T> start) throws CycleException {
        // perform the sort over the subgraph graph formed from the given vertex, calculating roots and references
        // for its children
        Map<Vertex<T>, AtomicInteger> vertexMap = new HashMap<>();
        List<Vertex<T>> vertices = DepthFirstTraversal.traverse(dag, start);
        for (Vertex<T> v : vertices) {
            List<Vertex<T>> outgoing = dag.getOutgoingAdjacentVertices(v);
            for (Vertex<T> child : outgoing) {
                AtomicInteger count = vertexMap.computeIfAbsent(child, k -> new AtomicInteger());
                count.incrementAndGet();
            }
        }

        List<Vertex<T>> roots = new ArrayList<>();
        roots.add(start);
        // perform the sort
        return sort(dag, vertexMap, roots);
    }

    /**
     * Performs a reverse topological sort of the subgraph reachable from the outgoing edges of the given vertex.
     */
    public List<Vertex<T>> reverseSort(DirectedGraph<T> dag) throws CycleException {
        List<Vertex<T>> sortSequence = sort(dag);
        Collections.reverse(sortSequence);
        return sortSequence;
    }

    /**
     * Performs a topological sort of the subgraph reachable from the outgoing edges of the given vertex.
     */
    public List<Vertex<T>> reverseSort(DirectedGraph<T> dag, Vertex<T> start) throws CycleException {
        List<Vertex<T>> sorted = sort(dag, start);
        Collections.reverse(sorted);
        return sorted;
    }

    /**
     * Performs the sort.
     *
     * @param dag the DAG to sort
     * @param vertices map of vertices and references
     * @param roots roots in the graph
     * @return the total ordering calculated by the topological sort
     * @throws CycleException if a cycle is detected
     */
    private List<Vertex<T>> sort(DirectedGraph<T> dag, Map<Vertex<T>, AtomicInteger> vertices, List<Vertex<T>> roots) throws CycleException {
        List<Vertex<T>> visited = new ArrayList<>();
        int num = vertices.size() + roots.size();
        while (!roots.isEmpty()) {
            Vertex<T> v = roots.remove(roots.size() - 1);
            visited.add(v);
            List<Vertex<T>> outgoing = dag.getOutgoingAdjacentVertices(v);
            for (Vertex<T> child : outgoing) {
                AtomicInteger count = vertices.get(child);
                if (count.decrementAndGet() == 0) {
                    // add child to root list as all parents are processed
                    roots.add(child);
                }
            }
        }
        if (visited.size() != num) {
            throw new CycleException();
        }
        return visited;
    }

}
