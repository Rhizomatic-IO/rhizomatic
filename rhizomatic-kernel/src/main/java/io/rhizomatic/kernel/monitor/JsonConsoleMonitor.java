package io.rhizomatic.kernel.monitor;

import io.rhizomatic.api.Monitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

/**
 * Dispatches to the console in JSON format.
 */
public class JsonConsoleMonitor implements Monitor {
    private Level level;

    public enum Level {
        SEVERE(2), INFO(1), DEBUG(0);

        final int value;

        Level(int value) {
            this.value = value;
        }
    }

    public JsonConsoleMonitor(Level level) {
        this.level = level;
    }

    public void severe(Supplier<String> supplier, Throwable... errors) {
        output("SEVERE", supplier.get(), errors);
    }


    @Override
    public void severe(String message, Throwable... errors) {
        output("SEVERE", message, errors);
    }

    public void info(Supplier<String> supplier, Throwable... errors) {
        if (Level.INFO.value < level.value) {
            return;
        }
        output("INFO", supplier.get(), errors);
    }

    @Override
    public void info(String message, Throwable... errors) {
        if (ConsoleMonitor.Level.INFO.value < level.value) {
            return;
        }
        output("INFO", message, errors);
    }

    public void debug(Supplier<String> supplier, Throwable... errors) {
        if (Level.DEBUG.value < level.value) {
            return;
        }
        output("DEBUG", supplier.get(), errors);
    }

    private void output(String level, String message, Throwable... errors) {
        var time = ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        var output = new StringBuilder("{");
        writeProperty("message", message, output);
        writeProperty("@timestamp", time, output);

        if (errors != null && errors.length > 0 && errors[0] != null) {
            writeProperty("level", level, output);
            var errorBuilder = new StringBuilder();
            writeProperty("exception_class", errors[0].getClass().getName(), errorBuilder);
            writeProperty("exception_message", errors[0].getMessage(), errorBuilder);

            var trace = new StringWriter();
            errors[0].printStackTrace(new PrintWriter(trace));
            writeTerminalProperty("stacktrace", trace.toString().replaceAll("[\n\r]", "").replaceAll("[\u0009]", " "), errorBuilder);
            writeObjectProperty("exception", errorBuilder.toString(), output);
        } else {
            writeTerminalProperty("level", level, output);
        }

        output.append("}");

        System.out.println(output.toString());
    }

    @SuppressWarnings("SameParameterValue")
    private void writeObjectProperty(String key, String value, StringBuilder builder) {
        builder.append("\"").append(key).append("\":{").append(value).append("}");
    }

    private void writeProperty(String key, String value, StringBuilder builder) {
        builder.append("\"").append(key).append("\":\"").append(value).append("\",");
    }

    private void writeTerminalProperty(String key, String value, StringBuilder builder) {
        builder.append("\"").append(key).append("\":\"").append(value).append("\"");
    }
}
