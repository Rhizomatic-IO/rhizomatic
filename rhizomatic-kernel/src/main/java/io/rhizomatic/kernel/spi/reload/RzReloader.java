package io.rhizomatic.kernel.spi.reload;

/**
 * Handles class addition and reloading events in the system. These events may result in injected instances and REST endpoints being re-evaluated.
 */
public interface RzReloader {

    /**
     * Signals a class definition has changed.
     */
    void classChanged(Class<?> clazz);

    /**
     * Signals a class has been added.
     */
    void classAdded(Class<?> clazz);

    /**
     * Registers a listener to receive reload event notifications.
     */
    void register(ReloadListener listener);
}
