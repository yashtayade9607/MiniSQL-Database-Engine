package com.novadb.exception;

/**
 * Base exception for all NovaDB exceptions.
 */
public class NovaDbException extends RuntimeException {
    public NovaDbException(String message) {
        super(message);
    }

    public NovaDbException(String message, Throwable cause) {
        super(message, cause);
    }
}
