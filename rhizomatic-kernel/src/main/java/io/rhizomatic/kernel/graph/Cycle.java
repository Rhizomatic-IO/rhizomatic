package io.rhizomatic.kernel.graph;

import java.util.List;

/**
 * Represents a cycle in a directed graph
 */
public class Cycle<T> {
    private List<Vertex<T>> originPath;
    private List<Vertex<T>> backPath;

    /**
     * Returns the list of vertices from the cycle origin to the endpoint.
     *
     * @return the list of vertices from the cycle origin to the endpoint
     */
    public List<Vertex<T>> getOriginPath() {
        return originPath;
    }

    /**
     * Sets the list of vertices from the cycle origin to the endpoint.
     *
     * @param originPath the list of vertices from the cycle origin to the endpoint
     */
    public void setOriginPath(List<Vertex<T>> originPath) {
        this.originPath = originPath;
    }

    /**
     * Returns the list of vertices from the cycle endpoint back to the origin.
     *
     * @return the the list of vertices from the cycle endpoint back to the origin
     */
    public List<Vertex<T>> getBackPath() {
        return backPath;
    }

    /**
     * Sets the list of vertices from the cycle endpoint back to the origin.
     *
     * @param backPath the the list of vertices from the cycle endpoint back to the origin
     */
    public void setBackPath(List<Vertex<T>> backPath) {
        this.backPath = backPath;
    }
}
