package com.novadb.parser.ast;

public record DropTableStatement(String tableName) implements Statement {}
