package io.rhizomatic.kernel.spi.scan;

import org.jetbrains.annotations.Nullable;

/**
 *
 */
public class Problem {
    public enum Type {
        ERROR, INFO
    }

    private Type type = Type.ERROR;
    private String description;
    private Exception exception;

    public Problem(Type type, String description, Exception exception) {
        this.type = type;
        this.description = description;
        this.exception = exception;
    }

    public Problem(Type type, String description) {
        this.type = type;
        this.description = description;
    }

    public Problem(String description) {
        this.description = description;
    }

    public Type getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    @Nullable
    public Exception getException() {
        return exception;
    }
}
