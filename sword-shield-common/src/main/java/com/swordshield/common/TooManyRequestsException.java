package com.swordshield.common;

/**
 * Signals that the client should back off (HTTP 429 semantics).
 * Mapped to {@link ErrorCode#TOO_MANY_REQUESTS} by {@link GlobalExceptionHandler}.
 */
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}

