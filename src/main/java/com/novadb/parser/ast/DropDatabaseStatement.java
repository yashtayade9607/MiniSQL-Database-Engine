package com.novadb.parser.ast;

public record DropDatabaseStatement(String databaseName) implements Statement {}
