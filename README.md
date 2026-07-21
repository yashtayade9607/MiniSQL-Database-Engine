# NovaDB

NovaDB is a lightweight relational database engine written entirely in Core Java 21, designed without any third-party database engines. It supports a subset of SQL, offering persistent storage, indexing, caching, basic transactions, and role-based security.

## Architecture

NovaDB is built adhering to SOLID principles and a clean architecture, modularized into distinct packages:

```
com.novadb
├── backup        # CSV export and ZIP-based automatic backups
├── cache         # In-memory LRU caching for frequently accessed tables
├── cli           # Interactive command-line shell (REPL)
├── exception     # Custom domain exceptions (SyntaxException, StorageException, etc.)
├── executor      # Query execution engine and Expression Evaluator
├── index         # HashMap-based equality indexes
├── logging       # Execution history and query timing logs
├── metadata      # CatalogManager managing database schemas and table metadata
├── model         # Core records/classes: Row, Column, TableInfo, DataType, Result
├── parser        # AST-based recursive descent SQL Parser
├── security      # Basic user authentication and role-based access control (Admin/ReadOnly)
├── storage       # Binary row serialization/deserialization and StorageManager
├── tokenizer     # Lexical analysis converting SQL strings into Tokens
└── util          # CLI result table formatting
```

## Storage Format

NovaDB uses a custom binary format for persisting data.
- **Metadata (`catalog.meta`)**: Binary encoded mapping of Databases -> Tables -> Columns.
- **Table Data (`dbName_tableName.dat`)**: Sequential binary log of rows.
  - Each row starts with a `boolean` (1 byte) indicating if it is active or deleted (for MVCC / deferred cleanup).
  - Followed by an `int` (4 bytes) denoting the length of the serialized row payload.
  - Followed by the binary serialized payload (custom `DataOutputStream` encoding per data type).

## Supported SQL Syntax

**DDL (Data Definition Language)**
- `CREATE DATABASE <name>;`
- `DROP DATABASE <name>;`
- `USE DATABASE <name>;` (or `USE <name>;`)
- `CREATE TABLE <name> (col1 INT PRIMARY KEY, col2 STRING NOT NULL, col3 BOOLEAN DEFAULT true);`
- `DROP TABLE <name>;`
- `SHOW TABLES;`
- `DESCRIBE TABLE <name>;` (or `DESCRIBE <name>;`)

**DML (Data Manipulation Language)**
- `INSERT INTO <name> (col1, col2) VALUES (val1, 'val2');`
- `SELECT col1, col2 FROM <name> WHERE col1 = 1 ORDER BY col2 DESC LIMIT 10;` (Supports `*`, `=`, `!=`, `>`, `<`, `>=`, `<=`, `AND`, `OR`, `LIKE`)
- `UPDATE <name> SET col1 = val1 WHERE col2 = val2;`
- `DELETE FROM <name> WHERE col1 = val1;`

**Transactions**
- `BEGIN;`
- `COMMIT;`
- `ROLLBACK;`

## Building and Running

Ensure you have Java 21 and Maven installed.

```bash
# Compile the project
mvn clean install

# Run the CLI
mvn exec:java
```

When prompted, login with the default credentials:
- Username: `admin`
- Password: `admin123`

## Limitations
- This is a lightweight educational engine. It does not implement B-Trees for range indexing; equality indexes use HashMaps.
- The SQL Parser uses recursive descent and supports a limited subset of SQL (no JOINs or subqueries yet).
- Concurrency uses a global `ReadWriteLock` for simplicity.
- The transaction model uses deferred execution instead of a full redo/undo Write-Ahead Log.

## Future Enhancements
- Add B-Tree indexing for efficient range queries (`>`, `<`).
- Implement nested queries and `JOIN` operations.
- Introduce an LRU Page Cache at the storage level instead of a row-based cache.
- Implement a true Write-Ahead Log (WAL) with ARIES recovery.
