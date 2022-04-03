package io.rhizomatic.kernel.monitor;

import io.rhizomatic.api.Monitor;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

/**
 * Dispatches to the console.
 */
public class ConsoleMonitor implements Monitor {
    private Level level;

    public enum Level {
        SEVERE(2), INFO(1), DEBUG(0);

        int value;

        Level(int value) {
            this.value = value;
        }
    }

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLACK = "\u001B[30m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_BLUE = "\u001B[34m";

    private boolean ansi;

    public ConsoleMonitor(Level level) {
        this.level = level;
        ansi = !System.getProperty("os.name").contains("Windows");
    }

    public void severe(Supplier<String> supplier, Throwable... errors) {
        output("SEVERE", supplier.get(), ANSI_RED, errors);
    }

    @Override
    public void severe(String message, Throwable... errors) {
        output("SEVERE", message, ANSI_RED, errors);
    }

    public void info(Supplier<String> supplier, Throwable... errors) {
        if (Level.INFO.value < level.value) {
            return;
        }
        output("INFO", supplier.get(), ANSI_BLUE, errors);
    }

    @Override
    public void info(String message, Throwable... errors) {
        if (Level.INFO.value < level.value) {
            return;
        }
        output("INFO", message, ANSI_BLUE, errors);
    }

    public void debug(Supplier<String> supplier, Throwable... errors) {
        if (Level.DEBUG.value < level.value) {
            return;
        }
        output("DEBUG", supplier.get(), ANSI_BLACK, errors);
    }

    private void output(String level, String message, String color, Throwable... errors) {
        color = ansi ? color : "";
        var reset = ansi ? ANSI_RESET : "";

        var time = ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        System.out.println(color + level + " " + time + " " + message + reset);
        if (errors != null) {
            for (Throwable error : errors) {
                if (error != null) {
                    error.printStackTrace(System.out);
                }
            }
        }
    }
}
