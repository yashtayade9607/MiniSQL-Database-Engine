package com.novadb.parser.ast;

import java.util.List;

public record InsertStatement(String tableName, List<String> columns, List<Expression> values) implements Statement {}
