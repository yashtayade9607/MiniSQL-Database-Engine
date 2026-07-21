package com.novadb.model;

import java.util.List;

public record Result(List<String> columnNames, List<Row> rows, String message) {
    public static Result ok(String message) {
        return new Result(null, null, message);
    }
}
