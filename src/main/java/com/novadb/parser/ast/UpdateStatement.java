package com.novadb.parser.ast;

import java.util.List;

public record UpdateStatement(String tableName, List<String> columns, List<Expression> values, Expression whereClause) implements Statement {}
