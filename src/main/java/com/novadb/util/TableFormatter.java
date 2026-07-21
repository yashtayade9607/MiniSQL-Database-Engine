package com.novadb.util;

import com.novadb.model.Result;
import com.novadb.model.Row;

import java.util.ArrayList;
import java.util.List;

public class TableFormatter {
    public static String format(Result result) {
        if (result.columnNames() == null || result.columnNames().isEmpty()) {
            return result.message();
        }

        List<String> headers = result.columnNames();
        List<Row> rows = result.rows();

        // Calculate column widths
        int[] colWidths = new int[headers.size()];
        for (int i = 0; i < headers.size(); i++) {
            colWidths[i] = headers.get(i).length();
        }

        List<String[]> stringRows = new ArrayList<>();
        for (Row row : rows) {
            String[] strRow = new String[headers.size()];
            for (int i = 0; i < headers.size(); i++) {
                Object val = row.get(headers.get(i));
                String s = val == null ? "NULL" : val.toString();
                strRow[i] = s;
                if (s.length() > colWidths[i]) {
                    colWidths[i] = s.length();
                }
            }
            stringRows.add(strRow);
        }

        StringBuilder sb = new StringBuilder();
        String separator = buildSeparator(colWidths);

        sb.append(separator).append("\n");
        sb.append(buildRow(headers.toArray(new String[0]), colWidths)).append("\n");
        sb.append(separator).append("\n");

        for (String[] row : stringRows) {
            sb.append(buildRow(row, colWidths)).append("\n");
        }
        sb.append(separator).append("\n");
        sb.append(rows.size()).append(" rows in set (").append(result.message()).append(")");

        return sb.toString();
    }

    private static String buildSeparator(int[] colWidths) {
        StringBuilder sb = new StringBuilder();
        sb.append("+");
        for (int width : colWidths) {
            sb.append("-".repeat(width + 2)).append("+");
        }
        return sb.toString();
    }

    private static String buildRow(String[] values, int[] colWidths) {
        StringBuilder sb = new StringBuilder();
        sb.append("|");
        for (int i = 0; i < values.length; i++) {
            sb.append(" ").append(padRight(values[i], colWidths[i])).append(" |");
        }
        return sb.toString();
    }

    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }
}
