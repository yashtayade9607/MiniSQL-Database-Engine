package com.novadb.model;

import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;

/**
 * Represents a single row in a table.
 */
public class Row {
    private final Map<String, Object> values;

    public Row(Map<String, Object> values) {
        this.values = new HashMap<>(values);
    }

    public Row() {
        this.values = new HashMap<>();
    }

    public void put(String columnName, Object value) {
        values.put(columnName, value);
    }

    public Object get(String columnName) {
        return values.get(columnName);
    }

    public Map<String, Object> getValues() {
        return Collections.unmodifiableMap(values);
    }

    @Override
    public String toString() {
        return "Row{" +
                "values=" + values +
                '}';
    }
}
