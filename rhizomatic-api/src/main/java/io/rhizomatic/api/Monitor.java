package io.rhizomatic.api;

import java.util.function.Supplier;

/**
 * System monitoring and logging interface. Applications can provide their own implementation using a JPMS services provided by a module.
 *
 *
 * For example, a module may contain the following provides clause:
 *
 * <pre>
 * module some.module {
 *    // ...
 *    provides io.rhizomatic.api.Monitor with CustomMonitor;
 * }
 * </pre>
 */
public interface Monitor {

    default void severe(Supplier<String> supplier, Throwable... errors) {
    }

    default void info(Supplier<String> supplier, Throwable... errors) {
    }

    default void debug(Supplier<String> supplier, Throwable... errors) {
    }

}
