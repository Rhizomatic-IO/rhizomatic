package io.rhizomatic.api;

/**
 * Main exception thrown by Rhizomatic.
 */
public class RhizomaticException extends RuntimeException {

    public RhizomaticException() {
    }

    public RhizomaticException(String message) {
        super(message);
    }

    public RhizomaticException(String message, Throwable cause) {
        super(message, cause);
    }

    public RhizomaticException(Throwable cause) {
        super(cause);
    }

    public RhizomaticException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
