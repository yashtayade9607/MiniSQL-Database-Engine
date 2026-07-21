package com.novadb.backup;

import com.novadb.exception.NovaDbException;
import com.novadb.model.Column;
import com.novadb.model.Row;
import com.novadb.model.TableInfo;
import com.novadb.storage.StorageManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupManager {
    private final String dataDir;

    public BackupManager(String dataDir) {
        this.dataDir = dataDir;
    }

    public void exportToCsv(String dbName, TableInfo tableInfo, StorageManager storage, String destCsvPath) {
        List<Row> rows = storage.scanTable(dbName, tableInfo);
        try (PrintWriter pw = new PrintWriter(new FileWriter(destCsvPath))) {
            // Write header
            String header = tableInfo.columns().stream()
                    .map(Column::name)
                    .collect(Collectors.joining(","));
            pw.println(header);

            // Write rows
            for (Row row : rows) {
                String line = tableInfo.columns().stream()
                        .map(col -> {
                            Object val = row.get(col.name());
                            return val == null ? "" : val.toString();
                        })
                        .collect(Collectors.joining(","));
                pw.println(line);
            }
        } catch (IOException e) {
            throw new NovaDbException("Failed to export to CSV", e);
        }
    }

    public void backupDatabase(String backupFilePath) {
        Path sourceDir = Paths.get(dataDir);
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backupFilePath))) {
            Files.walk(sourceDir).filter(path -> !Files.isDirectory(path)).forEach(path -> {
                ZipEntry zipEntry = new ZipEntry(sourceDir.relativize(path).toString());
                try {
                    zos.putNextEntry(zipEntry);
                    Files.copy(path, zos);
                    zos.closeEntry();
                } catch (IOException e) {
                    System.err.println("Failed to backup file: " + path);
                }
            });
        } catch (IOException e) {
            throw new NovaDbException("Failed to create backup", e);
        }
    }
}
