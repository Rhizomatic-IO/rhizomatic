package io.rhizomatic.kernel.monitor;

import io.rhizomatic.api.Monitor;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Dispatches to other monitors.
 */
public class MultiplexingMonitor implements Monitor {
    private Monitor[] monitors;

    public MultiplexingMonitor(Monitor... monitors) {
        Objects.requireNonNull(monitors, "Monitors cannot be null");
        this.monitors = monitors;
    }

    public void severe(Supplier<String> supplier, Throwable... errors) {
        for (Monitor monitor : monitors) {
            monitor.severe(supplier, errors);
        }
    }

    public void info(Supplier<String> supplier, Throwable... errors) {
        for (Monitor monitor : monitors) {
            monitor.info(supplier, errors);
        }
    }

    public void debug(Supplier<String> supplier, Throwable... errors) {
        for (Monitor monitor : monitors) {
            monitor.debug(supplier, errors);
        }
    }
}
