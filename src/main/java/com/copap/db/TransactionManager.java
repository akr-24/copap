package com.copap.db;

import java.sql.Connection;

public class TransactionManager {

    private final Connection connection;

    public TransactionManager(Connection connection) {
        this.connection = connection;
    }

    public <T> T executeInTransaction(TransactionCallback<T> callback) {
        try {
            connection.setAutoCommit(false);

            T result = callback.doInTransaction();

            connection.commit();
            return result;

        } catch (Exception e) {
            try {
                connection.rollback();
            } catch (Exception rollbackEx) {
                throw new RuntimeException("Rollback failed", rollbackEx);
            }
            throw new RuntimeException(e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
