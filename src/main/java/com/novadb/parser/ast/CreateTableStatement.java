package com.novadb.parser.ast;

import com.novadb.model.Column;
import java.util.List;

public record CreateTableStatement(String tableName, List<Column> columns) implements Statement {}
