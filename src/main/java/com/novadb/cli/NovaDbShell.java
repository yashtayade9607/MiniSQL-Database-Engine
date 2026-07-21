package com.novadb.cli;

import com.novadb.exception.NovaDbException;
import com.novadb.executor.QueryExecutor;
import com.novadb.metadata.CatalogManager;
import com.novadb.model.Result;
import com.novadb.parser.SqlParser;
import com.novadb.parser.ast.Statement;
import com.novadb.parser.ast.BeginStatement;
import com.novadb.parser.ast.CommitStatement;
import com.novadb.parser.ast.RollbackStatement;
import com.novadb.parser.ast.InsertStatement;
import com.novadb.parser.ast.UpdateStatement;
import com.novadb.parser.ast.DeleteStatement;
import com.novadb.storage.StorageManager;
import com.novadb.transaction.TransactionManager;
import com.novadb.security.UserManager;
import com.novadb.logging.QueryLogger;
import com.novadb.tokenizer.SqlTokenizer;
import com.novadb.tokenizer.Token;
import com.novadb.util.TableFormatter;

import java.io.File;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class NovaDbShell {
    private static final String DATA_DIR = "novadb_data";

    public static void main(String[] args) {
        System.out.println("Welcome to NovaDB!");
        
        Scanner scanner = new Scanner(System.in);
        UserManager userManager = new UserManager();
        UserManager.User currentUser = null;

        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        try {
            currentUser = userManager.authenticate(username, password);
            System.out.println("Logged in as " + currentUser.username() + " (" + currentUser.role() + ")");
        } catch (Exception e) {
            System.out.println("Authentication failed: " + e.getMessage());
            return;
        }

        System.out.println("Type your SQL commands. End them with a semicolon (;). Type 'exit' to quit.");

        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        CatalogManager catalog = new CatalogManager(DATA_DIR);
        StorageManager storage = new StorageManager(DATA_DIR);
        QueryExecutor executor = new QueryExecutor(catalog, storage);
        TransactionManager txManager = new TransactionManager();
        
        ReadWriteLock dbLock = new ReentrantReadWriteLock();

        StringBuilder queryBuilder = new StringBuilder();

        while (true) {
            if (queryBuilder.isEmpty()) {
                System.out.print("NovaDB> ");
            } else {
                System.out.print("      > ");
            }

            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine().trim();

            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                System.out.println("Bye.");
                break;
            }

            if (line.isEmpty()) continue;

            queryBuilder.append(line).append(" ");

            if (line.endsWith(";")) {
                String sql = queryBuilder.toString();
                queryBuilder.setLength(0); // clear

                long startTime = System.currentTimeMillis();
                boolean success = false;
                try {
                    SqlTokenizer tokenizer = new SqlTokenizer(sql);
                    List<Token> tokens = tokenizer.tokenize();
                    SqlParser parser = new SqlParser(tokens);
                    Statement stmt = parser.parse();

                    if (stmt != null) {
                        // Very simplistic role check: if read-only, only SELECT/SHOW/DESCRIBE are allowed
                        if (currentUser.role() == UserManager.Role.READ_ONLY && isWriteStatement(stmt)) {
                            throw new NovaDbException("Permission denied. Read-only user cannot execute write statements.");
                        }

                        dbLock.writeLock().lock();
                        try {
                            if (stmt instanceof BeginStatement) {
                                txManager.begin();
                                System.out.println("Transaction started.");
                            } else if (stmt instanceof CommitStatement) {
                                List<Statement> queued = txManager.commit();
                                for (Statement qStmt : queued) {
                                    executor.execute(qStmt); // apply deferred writes
                                }
                                System.out.println("Transaction committed.");
                            } else if (stmt instanceof RollbackStatement) {
                                txManager.rollback();
                                System.out.println("Transaction rolled back.");
                            } else if (txManager.isInTransaction() && isWriteStatement(stmt)) {
                                txManager.enqueue(stmt);
                                System.out.println("Statement queued.");
                            } else {
                                Result result = executor.execute(stmt);
                                System.out.println(TableFormatter.format(result));
                            }
                        } finally {
                            dbLock.writeLock().unlock();
                        }
                    }
                    success = true;
                } catch (NovaDbException e) {
                    System.err.println("Error: " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("Internal Error: " + e.getMessage());
                    e.printStackTrace();
                }
                long endTime = System.currentTimeMillis();
                long execTime = endTime - startTime;
                System.out.println("(" + execTime / 1000.0 + " sec)\n");
                
                QueryLogger.log(currentUser.username(), sql.trim(), execTime, success);
            }
        }
        scanner.close();
    }

    private static boolean isWriteStatement(Statement stmt) {
        return stmt instanceof InsertStatement || stmt instanceof UpdateStatement || stmt instanceof DeleteStatement;
    }
}
