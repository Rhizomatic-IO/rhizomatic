package io.rhizomatic.kernel.graph;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A directed graph.
 */
public class DirectedGraph<T> {
    private Map<Vertex<T>, VertexHolder> graphVertices;
    private Set<Edge<T>> graphEdges;

    public DirectedGraph() {
        graphVertices = new HashMap<>();
        graphEdges = new HashSet<>();
    }

    /**
     * Returns the vertices in the graph.
     */
    public Set<Vertex<T>> getVertices() {
        return graphVertices.keySet();
    }

    /**
     * Adds a vertex.
     */
    public Vertex<T> add(Vertex<T> vertex) {
        if (graphVertices.containsKey(vertex)) {
            return vertex;
        }
        graphVertices.put(vertex, new VertexHolder());
        return vertex;
    }

    /**
     * Adds a vertex.
     */
    public Vertex<T> addVertex(T entity) {
        return add(new Vertex<>(entity));
    }

    /**
     * Removes a vertex. Also removes any associated edges.
     */
    public Vertex<T> remove(Vertex<T> vertex) {
        List<Edge<T>> edges = new ArrayList<>(getOutgoingEdges(vertex));
        edges.forEach(this::removeEdge);
        graphVertices.remove(vertex);
        return vertex;
    }

    /**
     * Returns the adjacent vertices to the given vertex.
     */
    public Set<Vertex<T>> getAdjacentVertices(Vertex<T> vertex) {
        Set<Vertex<T>> adjacentVertices = new HashSet<>();
        Set<Edge<T>> incidentEdges = getOutgoingEdges(vertex);
        if (incidentEdges != null) {
            adjacentVertices.addAll(incidentEdges.stream().map(edge -> edge.getOppositeVertex(vertex)).collect(Collectors.toList()));
        }
        return adjacentVertices;
    }

    /**
     * Returns the adjacent vertices pointed to by outgoing edges for a given vertex.
     */
    public List<Vertex<T>> getOutgoingAdjacentVertices(Vertex<T> vertex) {
        return getAdjacentVertices(vertex, true);
    }

    /**
     * Returns the adjacent vertices pointed to by incoming edges for a given vertex.
     */
    public List<Vertex<T>> getIncomingAdjacentVertices(Vertex<T> vertex) {
        return getAdjacentVertices(vertex, false);
    }

    /**
     * Returns an edge between the two given vertices.
     */
    @Nullable
    public Edge<T> getEdge(Vertex<T> source, Vertex<T> sink) {
        Set<Edge<T>> edges = getOutgoingEdges(source);
        for (Edge<T> edge : edges) {
            if (edge.getSink() == sink) {
                return edge;
            }
        }
        return null;
    }

    /**
     * Returns the outgoing edges for the given vertex
     */
    public Set<Edge<T>> getOutgoingEdges(Vertex<T> vertex) {
        return graphVertices.get(vertex).getOutgoingEdges();
    }

    /**
     * Returns the incoming edges for the given vertex
     */
    public Set<Edge<T>> getIncomingEdges(Vertex<T> vertex) {
        return graphVertices.get(vertex).getIncomingEdges();
    }

    /**
     * Returns all edges in the graph.
     */
    public Set<Edge<T>> getEdges() {
        return graphEdges;
    }

    /**
     * Adds an edge.
     */
    public Edge<T> add(Edge<T> edge) {
        if (graphEdges.contains(edge)) {
            return edge;
        }
        Vertex<T> source = edge.getSource();
        Vertex<T> sink = edge.getSink();

        if (!graphVertices.containsKey(source)) {
            add(source);
        }
        if ((sink != source) && !graphVertices.containsKey(sink)) {
            add(sink);
        }
        Set<Edge<T>> sourceEdges = getOutgoingEdges(source);

        sourceEdges.add(edge);
        if (source != sink) {
            // avoid adding the edge a second time if the edge points back on itself
            Set<Edge<T>> sinkEdges = getIncomingEdges(sink);
            sinkEdges.add(edge);
        }

        graphEdges.add(edge);
        VertexHolder sourceHolder = graphVertices.get(edge.getSource());
        VertexHolder sinkHolder = graphVertices.get(edge.getSink());
        sourceHolder.getOutgoingEdges().add(edge);
        sinkHolder.getIncomingEdges().add(edge);
        return edge;
    }

    /**
     * Adds an edge.
     */
    public Edge<T> addEdge(Vertex<T> source, Vertex<T> sink) {
        return add(new Edge<>(source, sink));
    }

    /**
     * Removes an edge.
     */
    public Edge<T> remove(Edge<T> edge) {
        removeEdge(edge);
        Vertex<T> source = edge.getSource();
        Vertex<T> sink = edge.getSink();
        VertexHolder sourceHolder = graphVertices.get(source);
        VertexHolder sinkHolder = graphVertices.get(sink);
        // remove the edge from the source's outgoing edges
        sourceHolder.getOutgoingEdges().remove(edge);
        // remove the edge from the sink's incoming edges
        sinkHolder.getIncomingEdges().remove(edge);
        return edge;
    }

    private void removeEdge(Edge<T> edge) {
        // Remove the edge from the vertices incident edges.
        Vertex<T> source = edge.getSource();
        Set<Edge<T>> sourceEdges = getOutgoingEdges(source);
        sourceEdges.remove(edge);

        Vertex<T> sink = edge.getSink();
        Set<Edge<T>> sinkEdges = getIncomingEdges(sink);
        sinkEdges.remove(edge);

        // Remove the edge from edgeSet
        graphEdges.remove(edge);
    }

    /**
     * Returns the outgoing or incoming adjacent vertices for a given vertex
     */
    private List<Vertex<T>> getAdjacentVertices(Vertex<T> vertex, boolean outGoing) {
        List<Vertex<T>> adjacentVertices = new ArrayList<>();
        Set<Edge<T>> edges;
        if (outGoing) {
            edges = getOutgoingEdges(vertex);
        } else {
            edges = getIncomingEdges(vertex);
        }
        for (Edge<T> edge : edges) {
            Vertex<T> oppositeVertex = edge.getOppositeVertex(vertex);
            adjacentVertices.add(oppositeVertex);
        }
        return adjacentVertices;
    }

    private class VertexHolder {
        private Set<Edge<T>> incoming = new HashSet<>();
        private Set<Edge<T>> outgoingEdges = new HashSet<>();

        public Set<Edge<T>> getIncomingEdges() {
            return incoming;
        }

        public Set<Edge<T>> getOutgoingEdges() {
            return outgoingEdges;
        }

    }

}

