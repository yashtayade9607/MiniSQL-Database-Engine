package com.novadb.storage;

import com.novadb.exception.StorageException;
import com.novadb.model.Row;
import com.novadb.model.TableInfo;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles reading and writing rows to the disk.
 * File format per row: [is_active: 1 byte] [length: 4 bytes] [payload...]
 */
public class StorageManager {
    private final String dataDir;

    public StorageManager(String dataDir) {
        this.dataDir = dataDir;
    }

    private Path getTableFilePath(String dbName, String tableName) {
        return Paths.get(dataDir, dbName + "_" + tableName + ".dat");
    }

    /**
     * Appends a row to the table's file.
     * @return the offset where the row was written.
     */
    public long insertRow(String dbName, TableInfo tableInfo, Row row) {
        Path filePath = getTableFilePath(dbName, tableInfo.name());
        byte[] encoded = RowCodec.encode(row, tableInfo);

        try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "rw")) {
            long offset = raf.length();
            raf.seek(offset);
            raf.writeBoolean(true); // is_active
            raf.writeInt(encoded.length);
            raf.write(encoded);
            return offset;
        } catch (IOException e) {
            throw new StorageException("Failed to insert row", e);
        }
    }

    /**
     * Scans all active rows in the table.
     */
    public List<Row> scanTable(String dbName, TableInfo tableInfo) {
        Path filePath = getTableFilePath(dbName, tableInfo.name());
        List<Row> rows = new ArrayList<>();
        if (!filePath.toFile().exists()) {
            return rows;
        }

        try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r")) {
            long length = raf.length();
            while (raf.getFilePointer() < length) {
                boolean isActive = raf.readBoolean();
                int rowLen = raf.readInt();
                if (isActive) {
                    byte[] payload = new byte[rowLen];
                    raf.readFully(payload);
                    rows.add(RowCodec.decode(payload, tableInfo));
                } else {
                    raf.skipBytes(rowLen); // skip deleted row
                }
            }
        } catch (IOException e) {
            throw new StorageException("Failed to scan table", e);
        }
        return rows;
    }

    /**
     * Delete all rows that match a specific condition (simple implementation marks them as inactive).
     * This is a low-level operation, the executor will evaluate the condition.
     * For now, we will just provide a method to update the is_active flag given an offset.
     */
    public void markRowDeleted(String dbName, String tableName, long offset) {
        Path filePath = getTableFilePath(dbName, tableName);
        try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "rw")) {
            raf.seek(offset);
            raf.writeBoolean(false);
        } catch (IOException e) {
            throw new StorageException("Failed to delete row at offset " + offset, e);
        }
    }
    
    // We can also add a method to return (Offset, Row) pairs for easier updating/deleting by the executor
    public List<RowRecord> scanTableWithOffsets(String dbName, TableInfo tableInfo) {
        Path filePath = getTableFilePath(dbName, tableInfo.name());
        List<RowRecord> rows = new ArrayList<>();
        if (!filePath.toFile().exists()) {
            return rows;
        }

        try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r")) {
            long length = raf.length();
            while (raf.getFilePointer() < length) {
                long currentOffset = raf.getFilePointer();
                boolean isActive = raf.readBoolean();
                int rowLen = raf.readInt();
                if (isActive) {
                    byte[] payload = new byte[rowLen];
                    raf.readFully(payload);
                    rows.add(new RowRecord(currentOffset, RowCodec.decode(payload, tableInfo)));
                } else {
                    raf.skipBytes(rowLen);
                }
            }
        } catch (IOException e) {
            throw new StorageException("Failed to scan table", e);
        }
        return rows;
    }

    public record RowRecord(long offset, Row row) {}
}
