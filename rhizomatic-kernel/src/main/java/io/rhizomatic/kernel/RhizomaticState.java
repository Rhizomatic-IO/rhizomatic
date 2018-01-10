package io.rhizomatic.kernel;

/**
 *
 */
public enum RhizomaticState {
    /**
     * The system has not been initialized.
     */
    UNINITIALIZED,

    /**
     * The system is initialized and layers are loaded.
     */
    INITIALIZED,

    /**
     * The system is ready to receive requests.
     */
    STARTED,

    /**
     * The system has stopped processing requests and shutdown extensions.
     */
    SHUTDOWN,

    /**
     * The system is in an error state.
     */
    ERROR
}
