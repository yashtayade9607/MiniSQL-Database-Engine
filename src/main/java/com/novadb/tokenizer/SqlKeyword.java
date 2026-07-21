package com.novadb.tokenizer;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents SQL keywords supported by NovaDB.
 */
public enum SqlKeyword {
    CREATE, DROP, DATABASE, USE, TABLE, RENAME,
    INSERT, INTO, VALUES,
    SELECT, FROM, WHERE,
    UPDATE, SET,
    DELETE,
    SHOW, TABLES, DESCRIBE,
    AND, OR, LIKE,
    ORDER, BY, LIMIT,
    COUNT, SUM, AVG, MIN, MAX,
    INT, LONG, DOUBLE, FLOAT, BOOLEAN, STRING, DATE,
    PRIMARY, KEY, UNIQUE, NOT, NULL, DEFAULT,
    INDEX, BEGIN, COMMIT, ROLLBACK;

    private static final Set<String> KEYWORDS = Arrays.stream(values())
            .map(Enum::name)
            .collect(Collectors.toSet());

    /**
     * Checks if a string is a valid SQL keyword.
     * @param str the string to check
     * @return true if it's a keyword, false otherwise
     */
    public static boolean isKeyword(String str) {
        if (str == null) return false;
        return KEYWORDS.contains(str.toUpperCase());
    }
}
