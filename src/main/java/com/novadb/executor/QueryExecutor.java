package com.novadb.executor;

import com.novadb.exception.NovaDbException;
import com.novadb.metadata.CatalogManager;
import com.novadb.model.Column;
import com.novadb.model.Result;
import com.novadb.model.Row;
import com.novadb.model.TableInfo;
import com.novadb.parser.ast.*;
import com.novadb.storage.StorageManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class QueryExecutor {
    private final CatalogManager catalog;
    private final StorageManager storage;
    private String currentDatabase;

    public QueryExecutor(CatalogManager catalog, StorageManager storage) {
        this.catalog = catalog;
        this.storage = storage;
    }

    public Result execute(Statement stmt) {
        if (stmt instanceof CreateDatabaseStatement dbStmt) {
            catalog.createDatabase(dbStmt.databaseName());
            return Result.ok("Database created: " + dbStmt.databaseName());
        } else if (stmt instanceof DropDatabaseStatement dbStmt) {
            catalog.dropDatabase(dbStmt.databaseName());
            if (dbStmt.databaseName().equals(currentDatabase)) {
                currentDatabase = null;
            }
            return Result.ok("Database dropped: " + dbStmt.databaseName());
        } else if (stmt instanceof UseDatabaseStatement useStmt) {
            if (!catalog.databaseExists(useStmt.databaseName())) {
                throw new NovaDbException("Database not found: " + useStmt.databaseName());
            }
            currentDatabase = useStmt.databaseName();
            return Result.ok("Switched to database: " + currentDatabase);
        }

        checkDatabaseSelected();

        if (stmt instanceof CreateTableStatement createStmt) {
            TableInfo tableInfo = new TableInfo(createStmt.tableName(), createStmt.columns());
            catalog.createTable(currentDatabase, tableInfo);
            return Result.ok("Table created: " + createStmt.tableName());
        } else if (stmt instanceof DropTableStatement dropStmt) {
            catalog.dropTable(currentDatabase, dropStmt.tableName());
            return Result.ok("Table dropped: " + dropStmt.tableName());
        } else if (stmt instanceof ShowTablesStatement) {
            List<String> tables = catalog.listTables(currentDatabase);
            List<Row> rows = tables.stream().map(t -> {
                Row r = new Row();
                r.put("Tables_in_" + currentDatabase, t);
                return r;
            }).collect(Collectors.toList());
            return new Result(List.of("Tables_in_" + currentDatabase), rows, "OK");
        } else if (stmt instanceof DescribeTableStatement descStmt) {
            TableInfo tableInfo = catalog.getTable(currentDatabase, descStmt.tableName());
            if (tableInfo == null) throw new NovaDbException("Table not found: " + descStmt.tableName());
            List<Row> rows = tableInfo.columns().stream().map(c -> {
                Row r = new Row();
                r.put("Field", c.name());
                r.put("Type", c.type().name());
                r.put("Null", c.isNotNull() ? "NO" : "YES");
                r.put("Key", c.isPrimaryKey() ? "PRI" : "");
                r.put("Default", c.defaultValue());
                return r;
            }).collect(Collectors.toList());
            return new Result(List.of("Field", "Type", "Null", "Key", "Default"), rows, "OK");
        } else if (stmt instanceof InsertStatement insertStmt) {
            return executeInsert(insertStmt);
        } else if (stmt instanceof SelectStatement selectStmt) {
            return executeSelect(selectStmt);
        } else if (stmt instanceof UpdateStatement updateStmt) {
            return executeUpdate(updateStmt);
        } else if (stmt instanceof DeleteStatement deleteStmt) {
            return executeDelete(deleteStmt);
        } else {
            throw new NovaDbException("Unsupported statement type: " + stmt.getClass().getSimpleName());
        }
    }

    private void checkDatabaseSelected() {
        if (currentDatabase == null) {
            throw new NovaDbException("No database selected");
        }
    }

    private Result executeInsert(InsertStatement stmt) {
        TableInfo tableInfo = catalog.getTable(currentDatabase, stmt.tableName());
        if (tableInfo == null) throw new NovaDbException("Table not found: " + stmt.tableName());

        List<String> cols = stmt.columns().isEmpty() ? 
            tableInfo.columns().stream().map(Column::name).collect(Collectors.toList()) : stmt.columns();

        if (cols.size() != stmt.values().size()) {
            throw new NovaDbException("Column count doesn't match value count");
        }

        Row row = new Row();
        for (int i = 0; i < cols.size(); i++) {
            String colName = cols.get(i);
            Object value = ExpressionEvaluator.evaluate(stmt.values().get(i), new Row());
            // Basic type casting
            Column col = tableInfo.getColumn(colName);
            if (col == null) throw new NovaDbException("Unknown column: " + colName);
            row.put(colName, castValue(value, col));
        }

        // Apply defaults and check constraints
        for (Column col : tableInfo.columns()) {
            Object val = row.get(col.name());
            if (val == null && col.defaultValue() != null) {
                row.put(col.name(), castValue(col.defaultValue(), col));
            }
            if (col.isNotNull() && row.get(col.name()) == null) {
                throw new NovaDbException("Column cannot be null: " + col.name());
            }
        }

        // TODO: check primary key and unique constraints by scanning table
        storage.insertRow(currentDatabase, tableInfo, row);
        return Result.ok("1 row inserted");
    }

    private Result executeSelect(SelectStatement stmt) {
        TableInfo tableInfo = catalog.getTable(currentDatabase, stmt.tableName());
        if (tableInfo == null) throw new NovaDbException("Table not found: " + stmt.tableName());

        List<Row> rows = storage.scanTable(currentDatabase, tableInfo);

        // Filter
        if (stmt.whereClause() != null) {
            rows = rows.stream()
                .filter(r -> Boolean.TRUE.equals(ExpressionEvaluator.evaluate(stmt.whereClause(), r)))
                .collect(Collectors.toList());
        }

        // Order By
        if (stmt.orderBy() != null) {
            rows.sort((r1, r2) -> {
                Comparable c1 = (Comparable) r1.get(stmt.orderBy());
                Comparable c2 = (Comparable) r2.get(stmt.orderBy());
                if (c1 == null && c2 == null) return 0;
                if (c1 == null) return 1;
                if (c2 == null) return -1;
                int cmp = c1.compareTo(c2);
                return stmt.asc() ? cmp : -cmp;
            });
        }

        // Limit
        if (stmt.limit() != null && stmt.limit() < rows.size()) {
            rows = rows.subList(0, stmt.limit());
        }

        // Project columns
        List<String> resultCols = stmt.columns().contains("*") ? 
            tableInfo.columns().stream().map(Column::name).collect(Collectors.toList()) : stmt.columns();

        return new Result(resultCols, rows, rows.size() + " rows returned");
    }

    private Result executeUpdate(UpdateStatement stmt) {
        TableInfo tableInfo = catalog.getTable(currentDatabase, stmt.tableName());
        if (tableInfo == null) throw new NovaDbException("Table not found: " + stmt.tableName());

        List<StorageManager.RowRecord> records = storage.scanTableWithOffsets(currentDatabase, tableInfo);
        int updateCount = 0;

        for (StorageManager.RowRecord rec : records) {
            if (stmt.whereClause() == null || Boolean.TRUE.equals(ExpressionEvaluator.evaluate(stmt.whereClause(), rec.row()))) {
                // Delete old
                storage.markRowDeleted(currentDatabase, stmt.tableName(), rec.offset());
                
                // Construct new row
                Row newRow = new Row(rec.row().getValues());
                for (int i = 0; i < stmt.columns().size(); i++) {
                    String colName = stmt.columns().get(i);
                    Column col = tableInfo.getColumn(colName);
                    if (col == null) throw new NovaDbException("Unknown column: " + colName);
                    Object val = ExpressionEvaluator.evaluate(stmt.values().get(i), rec.row());
                    newRow.put(colName, castValue(val, col));
                }

                // Apply NOT NULL constraint checks
                for (Column col : tableInfo.columns()) {
                    if (col.isNotNull() && newRow.get(col.name()) == null) {
                        throw new NovaDbException("Column cannot be null: " + col.name());
                    }
                }

                // Insert new
                storage.insertRow(currentDatabase, tableInfo, newRow);
                updateCount++;
            }
        }
        return Result.ok(updateCount + " rows updated");
    }

    private Result executeDelete(DeleteStatement stmt) {
        TableInfo tableInfo = catalog.getTable(currentDatabase, stmt.tableName());
        if (tableInfo == null) throw new NovaDbException("Table not found: " + stmt.tableName());

        List<StorageManager.RowRecord> records = storage.scanTableWithOffsets(currentDatabase, tableInfo);
        int deleteCount = 0;

        for (StorageManager.RowRecord rec : records) {
            if (stmt.whereClause() == null || Boolean.TRUE.equals(ExpressionEvaluator.evaluate(stmt.whereClause(), rec.row()))) {
                storage.markRowDeleted(currentDatabase, stmt.tableName(), rec.offset());
                deleteCount++;
            }
        }
        return Result.ok(deleteCount + " rows deleted");
    }

    private Object castValue(Object val, Column col) {
        if (val == null) return null;
        String s = val.toString();
        return switch (col.type()) {
            case INT -> Integer.parseInt(s);
            case LONG -> Long.parseLong(s);
            case DOUBLE -> Double.parseDouble(s);
            case FLOAT -> Float.parseFloat(s);
            case BOOLEAN -> Boolean.parseBoolean(s);
            case STRING, DATE -> s;
        };
    }
}
