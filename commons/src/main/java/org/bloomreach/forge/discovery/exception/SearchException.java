package org.bloomreach.forge.discovery.exception;

public final class SearchException extends DiscoveryException {

    /** Sentinel for failures with no HTTP status (I/O errors, interruption, circuit-breaker open). */
    private static final int NO_STATUS_CODE = -1;

    private final int statusCode;

    public SearchException(String message) {
        super(message);
        this.statusCode = NO_STATUS_CODE;
    }

    public SearchException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = NO_STATUS_CODE;
    }

    public SearchException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }
}
