package com.novadb.exception;

/**
 * Thrown when there is a syntax error in the SQL query.
 */
public class SyntaxException extends NovaDbException {
    public SyntaxException(String message) {
        super(message);
    }
    
    public SyntaxException(String message, int line, int column) {
        super(String.format("Syntax error at %d:%d - %s", line, column, message));
    }
}
