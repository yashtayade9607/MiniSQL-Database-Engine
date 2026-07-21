package com.novadb.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages HashMap-based indexes for equality lookups.
 * In a real database, this would be persisted. For NovaDB lightweight engine, 
 * we will keep it in memory and rebuild on startup or use it as an ephemeral index.
 */
public class IndexManager {
    // Key: dbName_tableName_colName -> Map(colValue -> List<Offsets>)
    private final Map<String, Map<Object, List<Long>>> indexes = new HashMap<>();

    public void createIndex(String dbName, String tableName, String colName) {
        String key = buildKey(dbName, tableName, colName);
        if (!indexes.containsKey(key)) {
            indexes.put(key, new HashMap<>());
        }
    }

    public void addEntry(String dbName, String tableName, String colName, Object colValue, long offset) {
        String key = buildKey(dbName, tableName, colName);
        Map<Object, List<Long>> index = indexes.get(key);
        if (index != null) {
            index.computeIfAbsent(colValue, k -> new ArrayList<>()).add(offset);
        }
    }

    public List<Long> lookup(String dbName, String tableName, String colName, Object colValue) {
        String key = buildKey(dbName, tableName, colName);
        Map<Object, List<Long>> index = indexes.get(key);
        if (index != null) {
            return index.getOrDefault(colValue, new ArrayList<>());
        }
        return null;
    }

    public boolean hasIndex(String dbName, String tableName, String colName) {
        return indexes.containsKey(buildKey(dbName, tableName, colName));
    }

    private String buildKey(String db, String table, String col) {
        return db + "_" + table + "_" + col;
    }
}
