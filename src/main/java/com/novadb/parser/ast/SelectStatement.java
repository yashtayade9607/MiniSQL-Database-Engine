package com.novadb.parser.ast;

import java.util.List;

public record SelectStatement(List<String> columns, String tableName, Expression whereClause, String orderBy, boolean asc, Integer limit) implements Statement {}
