package com.novadb.parser;

import com.novadb.parser.ast.*;
import com.novadb.tokenizer.SqlTokenizer;
import com.novadb.tokenizer.Token;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqlParserTest {

    private Statement parseSql(String sql) {
        SqlTokenizer tokenizer = new SqlTokenizer(sql);
        List<Token> tokens = tokenizer.tokenize();
        SqlParser parser = new SqlParser(tokens);
        return parser.parse();
    }

    @Test
    void testCreateTable() {
        String sql = "CREATE TABLE users (id INT PRIMARY KEY, name STRING NOT NULL, age INT DEFAULT 18);";
        Statement stmt = parseSql(sql);
        assertInstanceOf(CreateTableStatement.class, stmt);
        CreateTableStatement create = (CreateTableStatement) stmt;
        assertEquals("users", create.tableName());
        assertEquals(3, create.columns().size());
        assertEquals("id", create.columns().get(0).name());
        assertTrue(create.columns().get(0).isPrimaryKey());
        assertEquals("18", create.columns().get(2).defaultValue());
    }

    @Test
    void testSelectStatement() {
        String sql = "SELECT id, name FROM users WHERE age > 18 ORDER BY name DESC LIMIT 10;";
        Statement stmt = parseSql(sql);
        assertInstanceOf(SelectStatement.class, stmt);
        SelectStatement select = (SelectStatement) stmt;
        assertEquals("users", select.tableName());
        assertEquals(2, select.columns().size());
        assertEquals("name", select.orderBy());
        assertFalse(select.asc());
        assertEquals(10, select.limit());
        assertInstanceOf(BinaryExpression.class, select.whereClause());
    }

    @Test
    void testInsertStatement() {
        String sql = "INSERT INTO users (id, name) VALUES (1, 'Alice');";
        Statement stmt = parseSql(sql);
        assertInstanceOf(InsertStatement.class, stmt);
        InsertStatement insert = (InsertStatement) stmt;
        assertEquals("users", insert.tableName());
        assertEquals(2, insert.columns().size());
        assertEquals(2, insert.values().size());
    }
}
