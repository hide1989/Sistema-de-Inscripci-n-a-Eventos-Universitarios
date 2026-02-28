package com.example.domain.exception;

/**
 * Base class for all domain-level exceptions.
 *
 * <p>Each subclass carries a stable {@code errorCode} string that the
 * {@code GlobalExceptionHandler} maps to the appropriate HTTP status and
 * {@code ErrorResponse} body — keeping HTTP concerns out of the domain.
 */
public abstract class DomainException extends RuntimeException {

    private final String errorCode;

    protected DomainException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
