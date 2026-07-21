package com.novadb.cache;

import com.novadb.model.Row;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TableCache {
    private final int capacity;
    
    // Key: dbName_tableName, Value: List of Rows
    private final Map<String, List<Row>> cache;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public TableCache(int capacity) {
        this.capacity = capacity;
        this.cache = new LinkedHashMap<>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, List<Row>> eldest) {
                return size() > TableCache.this.capacity;
            }
        };
    }

    public void put(String dbName, String tableName, List<Row> rows) {
        lock.writeLock().lock();
        try {
            cache.put(dbName + "_" + tableName, rows);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<Row> get(String dbName, String tableName) {
        lock.readLock().lock();
        try {
            return cache.get(dbName + "_" + tableName);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void invalidate(String dbName, String tableName) {
        lock.writeLock().lock();
        try {
            cache.remove(dbName + "_" + tableName);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
