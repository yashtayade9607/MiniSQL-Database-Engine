package com.novadb.metadata;

import com.novadb.exception.StorageException;
import com.novadb.model.Column;
import com.novadb.model.DataType;
import com.novadb.model.TableInfo;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the schemas of databases and tables.
 */
public class CatalogManager {
    private final String dataDir;
    // Map databaseName -> (Map tableName -> TableInfo)
    private final Map<String, Map<String, TableInfo>> catalogs = new HashMap<>();

    public CatalogManager(String dataDir) {
        this.dataDir = dataDir;
        initDataDir();
        loadCatalog();
    }

    private void initDataDir() {
        try {
            Files.createDirectories(Paths.get(dataDir));
        } catch (IOException e) {
            throw new StorageException("Failed to create data directory", e);
        }
    }

    public void createDatabase(String dbName) {
        if (catalogs.containsKey(dbName)) {
            throw new StorageException("Database already exists: " + dbName);
        }
        catalogs.put(dbName, new HashMap<>());
        saveCatalog();
    }
    
    public void dropDatabase(String dbName) {
        if (!catalogs.containsKey(dbName)) {
            throw new StorageException("Database not found: " + dbName);
        }
        catalogs.remove(dbName);
        saveCatalog();
        // Also remove physical files
        try {
            Files.list(Paths.get(dataDir)).filter(p -> p.getFileName().toString().startsWith(dbName + "_")).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            throw new StorageException("Failed to delete database files", e);
        }
    }

    public boolean databaseExists(String dbName) {
        return catalogs.containsKey(dbName);
    }

    public void createTable(String dbName, TableInfo tableInfo) {
        if (!catalogs.containsKey(dbName)) {
            throw new StorageException("Database not found: " + dbName);
        }
        Map<String, TableInfo> tables = catalogs.get(dbName);
        if (tables.containsKey(tableInfo.name())) {
            throw new StorageException("Table already exists: " + tableInfo.name());
        }
        tables.put(tableInfo.name(), tableInfo);
        saveCatalog();
    }

    public void dropTable(String dbName, String tableName) {
        Map<String, TableInfo> tables = catalogs.get(dbName);
        if (tables == null || !tables.containsKey(tableName)) {
            throw new StorageException("Table not found: " + tableName);
        }
        tables.remove(tableName);
        saveCatalog();
    }

    public TableInfo getTable(String dbName, String tableName) {
        Map<String, TableInfo> tables = catalogs.get(dbName);
        if (tables != null) {
            return tables.get(tableName);
        }
        return null;
    }

    public List<String> listTables(String dbName) {
        Map<String, TableInfo> tables = catalogs.get(dbName);
        if (tables == null) {
            throw new StorageException("Database not found: " + dbName);
        }
        return new ArrayList<>(tables.keySet());
    }

    private void saveCatalog() {
        Path catalogFile = Paths.get(dataDir, "catalog.meta");
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(catalogFile.toFile())))) {
            dos.writeInt(catalogs.size());
            for (Map.Entry<String, Map<String, TableInfo>> dbEntry : catalogs.entrySet()) {
                dos.writeUTF(dbEntry.getKey());
                Map<String, TableInfo> tables = dbEntry.getValue();
                dos.writeInt(tables.size());
                for (TableInfo table : tables.values()) {
                    dos.writeUTF(table.name());
                    dos.writeInt(table.columns().size());
                    for (Column col : table.columns()) {
                        dos.writeUTF(col.name());
                        dos.writeUTF(col.type().name());
                        dos.writeBoolean(col.isPrimaryKey());
                        dos.writeBoolean(col.isUnique());
                        dos.writeBoolean(col.isNotNull());
                        dos.writeBoolean(col.defaultValue() != null);
                        if (col.defaultValue() != null) {
                            dos.writeUTF(col.defaultValue());
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new StorageException("Failed to save catalog", e);
        }
    }

    private void loadCatalog() {
        Path catalogFile = Paths.get(dataDir, "catalog.meta");
        if (!Files.exists(catalogFile)) {
            return;
        }
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(catalogFile.toFile())))) {
            int numDbs = dis.readInt();
            for (int i = 0; i < numDbs; i++) {
                String dbName = dis.readUTF();
                Map<String, TableInfo> tables = new HashMap<>();
                int numTables = dis.readInt();
                for (int j = 0; j < numTables; j++) {
                    String tableName = dis.readUTF();
                    int numCols = dis.readInt();
                    List<Column> columns = new ArrayList<>();
                    for (int k = 0; k < numCols; k++) {
                        String colName = dis.readUTF();
                        DataType type = DataType.fromString(dis.readUTF());
                        boolean isPk = dis.readBoolean();
                        boolean isUnique = dis.readBoolean();
                        boolean isNotNull = dis.readBoolean();
                        boolean hasDefault = dis.readBoolean();
                        String defaultVal = hasDefault ? dis.readUTF() : null;
                        columns.add(new Column(colName, type, isPk, isUnique, isNotNull, defaultVal));
                    }
                    tables.put(tableName, new TableInfo(tableName, columns));
                }
                catalogs.put(dbName, tables);
            }
        } catch (IOException e) {
            throw new StorageException("Failed to load catalog", e);
        }
    }
}
