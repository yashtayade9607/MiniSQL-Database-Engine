package com.novadb.model;

/**
 * Represents a column definition in a table.
 * @param name The name of the column.
 * @param type The data type of the column.
 * @param isPrimaryKey Whether this column is a primary key.
 * @param isUnique Whether this column has a unique constraint.
 * @param isNotNull Whether this column cannot be null.
 * @param defaultValue The default value for the column (can be null).
 */
public record Column(
    String name,
    DataType type,
    boolean isPrimaryKey,
    boolean isUnique,
    boolean isNotNull,
    String defaultValue
) {
    public Column {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Column name cannot be null or blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("Column type cannot be null");
        }
    }
}
