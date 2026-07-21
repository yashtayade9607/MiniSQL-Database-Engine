package com.novadb.parser.ast;

/**
 * Base interface for all SQL statements.
 */
public sealed interface Statement permits
        CreateTableStatement,
        DropTableStatement,
        SelectStatement,
        InsertStatement,
        UpdateStatement,
        DeleteStatement,
        CreateDatabaseStatement,
        DropDatabaseStatement,
        UseDatabaseStatement,
        ShowTablesStatement,
        DescribeTableStatement,
        BeginStatement,
        CommitStatement,
        RollbackStatement {
}
