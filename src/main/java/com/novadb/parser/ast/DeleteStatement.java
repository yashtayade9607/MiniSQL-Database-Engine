package com.novadb.parser.ast;

public record DeleteStatement(String tableName, Expression whereClause) implements Statement {}
