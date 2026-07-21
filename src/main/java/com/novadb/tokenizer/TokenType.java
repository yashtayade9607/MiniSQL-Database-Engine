package com.novadb.tokenizer;

/**
 * Represents the types of tokens that the lexer can produce.
 */
public enum TokenType {
    KEYWORD,
    IDENTIFIER,
    STRING_LITERAL,
    NUMBER_LITERAL,
    BOOLEAN_LITERAL,
    OPERATOR,      // =, !=, >, <, >=, <=, +, -, *, /
    PUNCTUATION,   // (, ), ,, ;
    EOF,           // End of file / string
    UNKNOWN
}
