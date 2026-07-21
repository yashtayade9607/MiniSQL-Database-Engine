package com.novadb.parser.ast;

public record DescribeTableStatement(String tableName) implements Statement {}
