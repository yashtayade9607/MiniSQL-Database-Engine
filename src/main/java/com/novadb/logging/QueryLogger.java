package com.novadb.logging;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class QueryLogger {
    private static final String LOG_FILE = "novadb_query.log";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static void log(String username, String query, long executionTimeMs, boolean success) {
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             PrintWriter pw = new PrintWriter(fw)) {
            String timestamp = LocalDateTime.now().format(formatter);
            String status = success ? "SUCCESS" : "FAILED";
            pw.printf("[%s] USER: %s | STATUS: %s | TIME: %dms | QUERY: %s%n", 
                      timestamp, username, status, executionTimeMs, query);
        } catch (IOException e) {
            System.err.println("Failed to write to query log: " + e.getMessage());
        }
    }
}
