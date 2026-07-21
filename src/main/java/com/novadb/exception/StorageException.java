package com.novadb.exception;

/**
 * Thrown when an error occurs during file or storage operations.
 */
public class StorageException extends NovaDbException {
    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
