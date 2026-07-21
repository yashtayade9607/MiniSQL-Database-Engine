package com.novadb.model;

/**
 * Represents the supported data types in NovaDB.
 */
public enum DataType {
    INT,
    LONG,
    DOUBLE,
    FLOAT,
    BOOLEAN,
    STRING,
    DATE;

    /**
     * Parses a string representation of a data type into the enum.
     * @param typeName The string representation.
     * @return The DataType enum.
     * @throws IllegalArgumentException if the type is unknown.
     */
    public static DataType fromString(String typeName) {
        if (typeName == null) {
            throw new IllegalArgumentException("DataType name cannot be null");
        }
        return DataType.valueOf(typeName.toUpperCase());
    }
}
