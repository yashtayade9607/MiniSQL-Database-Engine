package com.novadb.transaction;

import com.novadb.exception.NovaDbException;
import com.novadb.parser.ast.Statement;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic transaction manager supporting BEGIN, COMMIT, and ROLLBACK.
 * Uses deferred execution: DML statements are queued and applied on COMMIT.
 */
public class TransactionManager {
    private boolean inTransaction = false;
    private final List<Statement> writeQueue = new ArrayList<>();

    public void begin() {
        if (inTransaction) {
            throw new NovaDbException("Transaction already in progress.");
        }
        inTransaction = true;
        writeQueue.clear();
    }

    public void enqueue(Statement stmt) {
        if (!inTransaction) {
            throw new NovaDbException("Not in a transaction.");
        }
        writeQueue.add(stmt);
    }

    public List<Statement> commit() {
        if (!inTransaction) {
            throw new NovaDbException("Not in a transaction.");
        }
        List<Statement> toExecute = new ArrayList<>(writeQueue);
        inTransaction = false;
        writeQueue.clear();
        return toExecute;
    }

    public void rollback() {
        if (!inTransaction) {
            throw new NovaDbException("Not in a transaction.");
        }
        inTransaction = false;
        writeQueue.clear();
    }

    public boolean isInTransaction() {
        return inTransaction;
    }
}
