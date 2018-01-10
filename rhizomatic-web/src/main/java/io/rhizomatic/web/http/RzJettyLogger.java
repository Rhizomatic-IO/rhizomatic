package io.rhizomatic.web.http;

import io.rhizomatic.api.Monitor;
import org.eclipse.jetty.util.log.Logger;

/**
 * Re-routes Jetty log messages to the system monitor.
 */
public class RzJettyLogger implements Logger {
    static Monitor MONITOR;

    public String getName() {
        return "Rhizomatic";
    }

    public void warn(String msg, Object... args) {
    }

    public void warn(Throwable thrown) {
        MONITOR.info(() -> "Error processing HTTP request", thrown);
    }

    public void warn(String msg, Throwable thrown) {
        MONITOR.severe(() -> msg, thrown);
    }

    public void info(String msg, Object... args) {

    }

    public void info(Throwable thrown) {

    }

    public void info(String msg, Throwable thrown) {

    }

    public boolean isDebugEnabled() {
        return false;
    }

    public void setDebugEnabled(boolean enabled) {

    }

    public void debug(String msg, Object... args) {

    }

    public void debug(String msg, long value) {

    }

    public void debug(Throwable thrown) {

    }

    public void debug(String msg, Throwable thrown) {

    }

    public Logger getLogger(String name) {
        return this;
    }

    public void ignore(Throwable ignored) {

    }

}
