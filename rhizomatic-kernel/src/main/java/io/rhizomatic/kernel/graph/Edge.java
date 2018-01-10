package io.rhizomatic.kernel.graph;

/**
 * Represents a directed edge in a graph.
 */
public class Edge<T> {
    private Vertex<T> source;
    private Vertex<T> sink;

    public Edge(Vertex<T> source, Vertex<T> sink) {
        this.source = source;
        this.sink = sink;
    }

    public Vertex<T> getSource() {
        return source;
    }

    public Vertex<T> getSink() {
        return sink;
    }

    public Vertex<T> getOppositeVertex(Vertex v) {
        if (this.source == v) {
            return this.sink;
        } else if (this.sink == v) {
            return this.source;
        } else {
            throw new AssertionError();
        }
    }
}

