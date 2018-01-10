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
        output("SEVERE", supplier, ANSI_RED, errors);
    }

    public void info(Supplier<String> supplier, Throwable... errors) {
        if (Level.INFO.value < level.value) {
            return;
        }
        output("INFO", supplier, ANSI_BLUE, errors);
    }

    public void debug(Supplier<String> supplier, Throwable... errors) {
        if (Level.DEBUG.value < level.value) {
            return;
        }
        output("DEBUG", supplier, ANSI_BLACK, errors);
    }

    private void output(String level, Supplier<String> supplier, String color, Throwable... errors) {
        color = ansi ? color : "";
        String reset = ansi ? ANSI_RESET : "";

        String time = ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        System.out.println(color + level + " " + time + " " + supplier.get() + reset);
        if (errors != null) {
            for (Throwable error : errors) {
                if (error != null) {
                    error.printStackTrace(System.out);
                }
            }
        }
    }
}
