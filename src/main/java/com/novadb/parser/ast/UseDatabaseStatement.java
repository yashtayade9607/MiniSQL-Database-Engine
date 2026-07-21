package com.novadb.parser.ast;

public record UseDatabaseStatement(String databaseName) implements Statement {}
