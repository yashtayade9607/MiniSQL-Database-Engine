package com.novadb.parser;

import com.novadb.exception.SyntaxException;
import com.novadb.model.Column;
import com.novadb.model.DataType;
import com.novadb.parser.ast.*;
import com.novadb.tokenizer.Token;
import com.novadb.tokenizer.TokenType;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses a list of Tokens into an Abstract Syntax Tree (Statement).
 */
public class SqlParser {
    private final List<Token> tokens;
    private int pos = 0;

    public SqlParser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public Statement parse() {
        if (isAtEnd()) return null;
        Statement stmt = parseStatement();
        if (match(TokenType.PUNCTUATION, ";")) {
            // optional trailing semicolon
        }
        if (!isAtEnd()) {
            throw error("Unexpected tokens after statement.");
        }
        return stmt;
    }

    private Statement parseStatement() {
        Token t = peek();
        if (t.type() == TokenType.KEYWORD) {
            return switch (t.value().toUpperCase()) {
                case "CREATE" -> parseCreate();
                case "DROP" -> parseDrop();
                case "USE" -> parseUse();
                case "SELECT" -> parseSelect();
                case "INSERT" -> parseInsert();
                case "UPDATE" -> parseUpdate();
                case "DELETE" -> parseDelete();
                case "SHOW" -> parseShow();
                case "DESCRIBE" -> parseDescribe();
                case "BEGIN" -> { consume(); yield new BeginStatement(); }
                case "COMMIT" -> { consume(); yield new CommitStatement(); }
                case "ROLLBACK" -> { consume(); yield new RollbackStatement(); }
                default -> throw error("Unexpected keyword: " + t.value());
            };
        }
        throw error("Expected a SQL statement (CREATE, SELECT, etc.)");
    }

    private Statement parseCreate() {
        consume(); // CREATE
        Token next = consume(TokenType.KEYWORD, "Expected 'DATABASE' or 'TABLE'");
        if (next.value().equalsIgnoreCase("DATABASE")) {
            String dbName = consume(TokenType.IDENTIFIER, "Expected database name").value();
            return new CreateDatabaseStatement(dbName);
        } else if (next.value().equalsIgnoreCase("TABLE")) {
            String tableName = consume(TokenType.IDENTIFIER, "Expected table name").value();
            consume(TokenType.PUNCTUATION, "(", "Expected '('");
            List<Column> columns = new ArrayList<>();
            do {
                columns.add(parseColumnDefinition());
            } while (match(TokenType.PUNCTUATION, ","));
            consume(TokenType.PUNCTUATION, ")", "Expected ')'");
            return new CreateTableStatement(tableName, columns);
        }
        throw error("Expected DATABASE or TABLE after CREATE");
    }

    private Column parseColumnDefinition() {
        String name = consume(TokenType.IDENTIFIER, "Expected column name").value();
        Token typeToken = consume(TokenType.KEYWORD, "Expected data type");
        DataType type;
        try {
            type = DataType.fromString(typeToken.value());
        } catch (IllegalArgumentException e) {
            throw error("Unknown data type: " + typeToken.value());
        }

        boolean isPk = false;
        boolean isUnique = false;
        boolean isNotNull = false;
        String defaultValue = null;

        while (true) {
            if (matchKeyword("PRIMARY")) {
                consumeKeyword("KEY", "Expected 'KEY' after 'PRIMARY'");
                isPk = true;
            } else if (matchKeyword("UNIQUE")) {
                isUnique = true;
            } else if (matchKeyword("NOT")) {
                consumeKeyword("NULL", "Expected 'NULL' after 'NOT'");
                isNotNull = true;
            } else if (matchKeyword("DEFAULT")) {
                Token val = consume();
                if (val.type() == TokenType.STRING_LITERAL || val.type() == TokenType.NUMBER_LITERAL || val.type() == TokenType.BOOLEAN_LITERAL) {
                    defaultValue = val.value();
                } else {
                    throw error("Expected literal after DEFAULT");
                }
            } else {
                break;
            }
        }
        return new Column(name, type, isPk, isUnique, isNotNull, defaultValue);
    }

    private Statement parseDrop() {
        consume(); // DROP
        Token next = consume(TokenType.KEYWORD, "Expected 'DATABASE' or 'TABLE'");
        if (next.value().equalsIgnoreCase("DATABASE")) {
            String dbName = consume(TokenType.IDENTIFIER, "Expected database name").value();
            return new DropDatabaseStatement(dbName);
        } else if (next.value().equalsIgnoreCase("TABLE")) {
            String tableName = consume(TokenType.IDENTIFIER, "Expected table name").value();
            return new DropTableStatement(tableName);
        }
        throw error("Expected DATABASE or TABLE after DROP");
    }

    private Statement parseUse() {
        consume(); // USE
        if (matchKeyword("DATABASE")) {
            // Optional DATABASE keyword
        }
        String dbName = consume(TokenType.IDENTIFIER, "Expected database name").value();
        return new UseDatabaseStatement(dbName);
    }

    private Statement parseShow() {
        consume(); // SHOW
        consumeKeyword("TABLES", "Expected 'TABLES'");
        return new ShowTablesStatement();
    }
    
    private Statement parseDescribe() {
        consume(); // DESCRIBE
        if (matchKeyword("TABLE")) {
            // optional TABLE keyword
        }
        String tableName = consume(TokenType.IDENTIFIER, "Expected table name").value();
        return new DescribeTableStatement(tableName);
    }

    private Statement parseSelect() {
        consume(); // SELECT
        List<String> cols = new ArrayList<>();
        if (match(TokenType.OPERATOR, "*")) {
            cols.add("*");
        } else {
            do {
                cols.add(consume(TokenType.IDENTIFIER, "Expected column name").value());
            } while (match(TokenType.PUNCTUATION, ","));
        }

        consumeKeyword("FROM", "Expected 'FROM'");
        String tableName = consume(TokenType.IDENTIFIER, "Expected table name").value();

        Expression whereClause = null;
        if (matchKeyword("WHERE")) {
            whereClause = parseExpression();
        }

        String orderBy = null;
        boolean asc = true;
        if (matchKeyword("ORDER")) {
            consumeKeyword("BY", "Expected 'BY'");
            orderBy = consume(TokenType.IDENTIFIER, "Expected column name").value();
            if (matchKeyword("DESC")) {
                asc = false;
            } else if (matchKeyword("ASC")) {
                asc = true;
            }
        }

        Integer limit = null;
        if (matchKeyword("LIMIT")) {
            Token num = consume(TokenType.NUMBER_LITERAL, "Expected integer limit");
            limit = Integer.parseInt(num.value());
        }

        return new SelectStatement(cols, tableName, whereClause, orderBy, asc, limit);
    }

    private Statement parseInsert() {
        consume(); // INSERT
        consumeKeyword("INTO", "Expected 'INTO'");
        String tableName = consume(TokenType.IDENTIFIER, "Expected table name").value();

        List<String> columns = new ArrayList<>();
        if (match(TokenType.PUNCTUATION, "(")) {
            do {
                columns.add(consume(TokenType.IDENTIFIER, "Expected column name").value());
            } while (match(TokenType.PUNCTUATION, ","));
            consume(TokenType.PUNCTUATION, ")", "Expected ')'");
        }

        consumeKeyword("VALUES", "Expected 'VALUES'");
        consume(TokenType.PUNCTUATION, "(", "Expected '('");
        List<Expression> values = new ArrayList<>();
        do {
            values.add(parseExpression());
        } while (match(TokenType.PUNCTUATION, ","));
        consume(TokenType.PUNCTUATION, ")", "Expected ')'");

        return new InsertStatement(tableName, columns, values);
    }

    private Statement parseUpdate() {
        consume(); // UPDATE
        String tableName = consume(TokenType.IDENTIFIER, "Expected table name").value();
        consumeKeyword("SET", "Expected 'SET'");

        List<String> columns = new ArrayList<>();
        List<Expression> values = new ArrayList<>();
        do {
            columns.add(consume(TokenType.IDENTIFIER, "Expected column name").value());
            consume(TokenType.OPERATOR, "=", "Expected '='");
            values.add(parseExpression());
        } while (match(TokenType.PUNCTUATION, ","));

        Expression whereClause = null;
        if (matchKeyword("WHERE")) {
            whereClause = parseExpression();
        }

        return new UpdateStatement(tableName, columns, values, whereClause);
    }

    private Statement parseDelete() {
        consume(); // DELETE
        consumeKeyword("FROM", "Expected 'FROM'");
        String tableName = consume(TokenType.IDENTIFIER, "Expected table name").value();
        Expression whereClause = null;
        if (matchKeyword("WHERE")) {
            whereClause = parseExpression();
        }
        return new DeleteStatement(tableName, whereClause);
    }

    // --- Expression Parsing (very basic recursive descent) ---

    private Expression parseExpression() {
        return parseOr();
    }

    private Expression parseOr() {
        Expression expr = parseAnd();
        while (matchKeyword("OR")) {
            Expression right = parseAnd();
            expr = new BinaryExpression(expr, "OR", right);
        }
        return expr;
    }

    private Expression parseAnd() {
        Expression expr = parseEquality();
        while (matchKeyword("AND")) {
            Expression right = parseEquality();
            expr = new BinaryExpression(expr, "AND", right);
        }
        return expr;
    }

    private Expression parseEquality() {
        Expression expr = parseComparison();
        while (match(TokenType.OPERATOR, "=") || match(TokenType.OPERATOR, "!=") || matchKeyword("LIKE")) {
            String op = previous().value().toUpperCase();
            Expression right = parseComparison();
            expr = new BinaryExpression(expr, op, right);
        }
        return expr;
    }

    private Expression parseComparison() {
        Expression expr = parsePrimary();
        while (match(TokenType.OPERATOR, ">") || match(TokenType.OPERATOR, ">=") 
            || match(TokenType.OPERATOR, "<") || match(TokenType.OPERATOR, "<=")) {
            String op = previous().value();
            Expression right = parsePrimary();
            expr = new BinaryExpression(expr, op, right);
        }
        return expr;
    }

    private Expression parsePrimary() {
        if (match(TokenType.STRING_LITERAL) || match(TokenType.NUMBER_LITERAL) || match(TokenType.BOOLEAN_LITERAL)) {
            Token t = previous();
            Object val = t.value();
            if (t.type() == TokenType.NUMBER_LITERAL) {
                if (((String)val).contains(".")) {
                    val = Double.parseDouble((String)val);
                } else {
                    val = Long.parseLong((String)val);
                }
            } else if (t.type() == TokenType.BOOLEAN_LITERAL) {
                val = Boolean.parseBoolean((String)val);
            }
            return new LiteralExpression(val);
        }
        if (match(TokenType.IDENTIFIER)) {
            return new IdentifierExpression(previous().value());
        }
        throw error("Expected expression.");
    }

    // --- Helpers ---

    private boolean match(TokenType type, String val) {
        if (check(type) && peek().value().equals(val)) {
            consume();
            return true;
        }
        return false;
    }

    private boolean match(TokenType type) {
        if (check(type)) {
            consume();
            return true;
        }
        return false;
    }
    
    private boolean matchKeyword(String keyword) {
        if (check(TokenType.KEYWORD) && peek().value().equalsIgnoreCase(keyword)) {
            consume();
            return true;
        }
        return false;
    }

    private void consumeKeyword(String keyword, String errorMsg) {
        if (matchKeyword(keyword)) return;
        throw error(errorMsg);
    }

    private Token consume(TokenType type, String val, String errorMsg) {
        if (check(type) && peek().value().equals(val)) return consume();
        throw error(errorMsg);
    }

    private Token consume(TokenType type, String errorMsg) {
        if (check(type)) return consume();
        throw error(errorMsg);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type() == type;
    }

    private Token consume() {
        if (!isAtEnd()) pos++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type() == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(pos);
    }

    private Token previous() {
        return tokens.get(pos - 1);
    }

    private SyntaxException error(String message) {
        Token t = peek();
        return new SyntaxException(message, t.line(), t.column());
    }
}
