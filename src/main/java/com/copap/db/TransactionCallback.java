package com.copap.db;

@FunctionalInterface
public interface TransactionCallback<T> {
    T doInTransaction();
}