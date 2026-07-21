package com.novadb.parser.ast;

public record CreateDatabaseStatement(String databaseName) implements Statement {}
