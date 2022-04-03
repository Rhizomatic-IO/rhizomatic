package io.rhizomatic.kernel.graph;

/**
 * Default vertex implementation
 */
public class Vertex<T> {
    private T entity;

    /**
     * Constructor.
     *
     * @param entity entity that the vertex represents
     */
    public Vertex(T entity) {
        this.entity = entity;
    }

    public T getEntity() {
        return entity;
    }


    @SuppressWarnings("rawtypes")
    public boolean equals(Object obj) {
        return obj instanceof Vertex && getEntity().equals(((Vertex) obj).getEntity());
    }
}
