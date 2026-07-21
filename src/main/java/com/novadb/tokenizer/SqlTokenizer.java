package com.novadb.tokenizer;

import com.novadb.exception.SyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tokenizes SQL queries into a list of Tokens.
 */
public class SqlTokenizer {
    private final String input;
    private int pos = 0;
    private int line = 1;
    private int column = 1;

    public SqlTokenizer(String input) {
        this.input = input == null ? "" : input;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (pos < input.length()) {
            char c = peek();

            if (Character.isWhitespace(c)) {
                consume();
            } else if (c == '-' && peek(1) == '-') {
                // single line comment
                consume();
                consume();
                while (pos < input.length() && peek() != '\n') {
                    consume();
                }
            } else if (Character.isLetter(c)) {
                tokens.add(readIdentifierOrKeyword());
            } else if (Character.isDigit(c)) {
                tokens.add(readNumber());
            } else if (c == '\'') {
                tokens.add(readStringLiteral());
            } else if (isOperatorChar(c)) {
                tokens.add(readOperator());
            } else if (isPunctuation(c)) {
                tokens.add(new Token(TokenType.PUNCTUATION, String.valueOf(consume()), line, column - 1));
            } else {
                throw new SyntaxException("Unknown character: '" + c + "'", line, column);
            }
        }
        tokens.add(new Token(TokenType.EOF, "", line, column));
        return tokens;
    }

    private Token readIdentifierOrKeyword() {
        int startLine = line;
        int startCol = column;
        StringBuilder sb = new StringBuilder();

        while (pos < input.length() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            sb.append(consume());
        }

        String val = sb.toString();
        if (SqlKeyword.isKeyword(val)) {
            return new Token(TokenType.KEYWORD, val.toUpperCase(), startLine, startCol);
        } else if (val.equalsIgnoreCase("TRUE") || val.equalsIgnoreCase("FALSE")) {
            return new Token(TokenType.BOOLEAN_LITERAL, val.toUpperCase(), startLine, startCol);
        } else {
            return new Token(TokenType.IDENTIFIER, val, startLine, startCol);
        }
    }

    private Token readNumber() {
        int startLine = line;
        int startCol = column;
        StringBuilder sb = new StringBuilder();

        boolean hasDot = false;
        while (pos < input.length()) {
            char c = peek();
            if (Character.isDigit(c)) {
                sb.append(consume());
            } else if (c == '.' && !hasDot) {
                hasDot = true;
                sb.append(consume());
            } else {
                break;
            }
        }

        return new Token(TokenType.NUMBER_LITERAL, sb.toString(), startLine, startCol);
    }

    private Token readStringLiteral() {
        int startLine = line;
        int startCol = column;
        consume(); // consume opening quote
        StringBuilder sb = new StringBuilder();

        while (pos < input.length()) {
            char c = peek();
            if (c == '\'') {
                consume(); // consume closing quote
                break;
            } else {
                sb.append(consume());
            }
        }

        return new Token(TokenType.STRING_LITERAL, sb.toString(), startLine, startCol);
    }

    private Token readOperator() {
        int startLine = line;
        int startCol = column;
        char c1 = consume();
        String op = String.valueOf(c1);

        if (pos < input.length()) {
            char c2 = peek();
            if ((c1 == '!' && c2 == '=') || (c1 == '>' && c2 == '=') || (c1 == '<' && c2 == '=')) {
                op += consume();
            }
        }

        if (op.equals("!") && op.length() == 1) {
            throw new SyntaxException("Invalid operator '!' without '='", startLine, startCol);
        }

        return new Token(TokenType.OPERATOR, op, startLine, startCol);
    }

    private boolean isOperatorChar(char c) {
        return c == '=' || c == '!' || c == '>' || c == '<' || c == '+' || c == '-' || c == '*' || c == '/';
    }

    private boolean isPunctuation(char c) {
        return c == '(' || c == ')' || c == ',' || c == ';';
    }

    private char peek() {
        return input.charAt(pos);
    }

    private char peek(int lookahead) {
        if (pos + lookahead >= input.length()) {
            return '\0';
        }
        return input.charAt(pos + lookahead);
    }

    private char consume() {
        char c = input.charAt(pos++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }
}
