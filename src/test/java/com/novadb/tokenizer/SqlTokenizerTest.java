package com.novadb.tokenizer;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SqlTokenizerTest {

    @Test
    void testBasicSelect() {
        String sql = "SELECT id, name FROM users WHERE age >= 18;";
        SqlTokenizer tokenizer = new SqlTokenizer(sql);
        List<Token> tokens = tokenizer.tokenize();

        assertEquals(11, tokens.size()); // includes EOF
        assertEquals(TokenType.KEYWORD, tokens.get(0).type());
        assertEquals("SELECT", tokens.get(0).value());

        assertEquals(TokenType.IDENTIFIER, tokens.get(1).type());
        assertEquals("id", tokens.get(1).value());

        assertEquals(TokenType.PUNCTUATION, tokens.get(2).type());
        assertEquals(",", tokens.get(2).value());

        assertEquals(TokenType.IDENTIFIER, tokens.get(3).type());
        assertEquals("name", tokens.get(3).value());

        assertEquals(TokenType.KEYWORD, tokens.get(4).type());
        assertEquals("FROM", tokens.get(4).value());

        assertEquals(TokenType.IDENTIFIER, tokens.get(5).type());
        assertEquals("users", tokens.get(5).value());

        assertEquals(TokenType.KEYWORD, tokens.get(6).type());
        assertEquals("WHERE", tokens.get(6).value());

        assertEquals(TokenType.IDENTIFIER, tokens.get(7).type());
        assertEquals("age", tokens.get(7).value());

        assertEquals(TokenType.OPERATOR, tokens.get(8).type());
        assertEquals(">=", tokens.get(8).value());

        assertEquals(TokenType.NUMBER_LITERAL, tokens.get(9).type());
        assertEquals("18", tokens.get(9).value());

        assertEquals(TokenType.PUNCTUATION, tokens.get(10).type());
        assertEquals(";", tokens.get(10).value());
    }

    @Test
    void testStringLiteral() {
        String sql = "INSERT INTO users VALUES ('John Doe', 25);";
        SqlTokenizer tokenizer = new SqlTokenizer(sql);
        List<Token> tokens = tokenizer.tokenize();

        assertEquals(9, tokens.size());
        assertEquals(TokenType.STRING_LITERAL, tokens.get(4).type());
        assertEquals("John Doe", tokens.get(4).value());
    }

    @Test
    void testComments() {
        String sql = "SELECT * FROM users -- this is a comment\n WHERE id = 1;";
        SqlTokenizer tokenizer = new SqlTokenizer(sql);
        List<Token> tokens = tokenizer.tokenize();

        // SELECT, *, FROM, users, WHERE, id, =, 1, ;
        // * is an operator here
        assertEquals(10, tokens.size());
        assertEquals("WHERE", tokens.get(4).value());
    }
}
