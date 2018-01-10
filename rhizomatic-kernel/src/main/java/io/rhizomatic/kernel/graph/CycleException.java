package io.rhizomatic.kernel.graph;

/**
 * Thrown when a cycle in a DAG is encountered.
 */
public class CycleException extends RuntimeException {

    public CycleException() {
        super("Cycle detected");
    }

}