package com.novadb.tokenizer;

/**
 * Represents a single token extracted from the SQL string.
 * @param type The type of the token.
 * @param value The string value of the token.
 * @param line The line number where the token was found.
 * @param column The column number where the token was found.
 */
public record Token(TokenType type, String value, int line, int column) {
    @Override
    public String toString() {
        return String.format("Token{type=%s, value='%s', pos=%d:%d}", type, value, line, column);
    }
}
