package com.novadb.model;

import java.util.Collections;
import java.util.List;

/**
 * Represents the schema information of a table.
 * @param name The name of the table.
 * @param columns The list of columns in the table.
 */
public record TableInfo(String name, List<Column> columns) {
    public TableInfo {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Table name cannot be null or blank");
        }
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("Table must have at least one column");
        }
        columns = List.copyOf(columns);
    }
    
    /**
     * Gets a column by its name.
     * @param columnName the column name to search for
     * @return the Column if found, else null
     */
    public Column getColumn(String columnName) {
        for (Column col : columns) {
            if (col.name().equalsIgnoreCase(columnName)) {
                return col;
            }
        }
        return null;
    }
}
