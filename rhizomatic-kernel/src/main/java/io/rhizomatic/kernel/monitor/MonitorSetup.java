package io.rhizomatic.kernel.monitor;

import io.rhizomatic.api.Monitor;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static io.rhizomatic.kernel.spi.ConfigurationKeys.CONSOLE_DISABLE;
import static io.rhizomatic.kernel.spi.ConfigurationKeys.MONITOR_JSON;
import static io.rhizomatic.kernel.spi.ConfigurationKeys.MONITOR_JSON_LOWER;
import static java.util.stream.Collectors.toList;

/**
 *
 */
public class MonitorSetup {

    public static Monitor initializeMonitor(Map<String, Object> configuration) {
        Monitor monitor;

        List<Monitor> monitorExtensions = ServiceLoader.load(Monitor.class).stream().map(ServiceLoader.Provider::get).collect(toList());

        if (getBooleanProperty(MONITOR_JSON)) {
            Monitor jsonMonitor = new JsonConsoleMonitor(JsonConsoleMonitor.Level.INFO);
            monitorExtensions.add(0, jsonMonitor);
        } else if (!Boolean.parseBoolean((String) configuration.get(CONSOLE_DISABLE))) {
            Monitor consoleMonitor = new ConsoleMonitor(ConsoleMonitor.Level.INFO);
            monitorExtensions.add(0, consoleMonitor);
        }
        // add supplied monitors
        if (monitorExtensions.isEmpty()) {
            monitor = new Monitor() {
            };
        } else if (monitorExtensions.size() == 1) {
            monitor = monitorExtensions.get(0);
        } else {
            monitor = new MultiplexingMonitor(monitorExtensions.toArray(new Monitor[0]));
        }
        redirectJdkLogging(monitor);


        return monitor;
    }

    public static void redirectJdkLogging(Monitor monitor) {
        // redirect JDK logging
        Logger globalLogger = Logger.getLogger("");
        Handler[] handlers = globalLogger.getHandlers();
        for (Handler handler : handlers) {
            globalLogger.removeHandler(handler);
        }
        globalLogger.addHandler(new Handler() {

            public void publish(LogRecord record) {
                if (Level.SEVERE == record.getLevel()) {
                    monitor.severe(record::getMessage, record.getThrown());
                } else if (Level.WARNING == record.getLevel()) {
                    monitor.debug(record::getMessage, record.getThrown());
                } else if (Level.INFO == record.getLevel()) {
                    monitor.debug(record::getMessage, record.getThrown());
                } else {
                    monitor.debug(record::getMessage, record.getThrown());
                }
            }

            public void flush() {

            }

            public void close() throws SecurityException {

            }
        });
    }

    @SuppressWarnings("SameParameterValue")
    private static boolean getBooleanProperty(String key) {
        String value = System.getenv(key);
        if (value == null) {
            value = System.getProperty(key);
            if (value == null) {
                value = System.getProperty(MONITOR_JSON_LOWER);
            }
        }
        return Boolean.parseBoolean(value);
    }

}
