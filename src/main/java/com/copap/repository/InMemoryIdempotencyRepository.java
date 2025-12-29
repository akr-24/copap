package com.copap.repository;

import com.copap.model.IdempotencyRecord;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryIdempotencyRepository implements IdempotencyRepository {

    private final ConcurrentMap<String, IdempotencyRecord> store =
            new ConcurrentHashMap<>();

    @Override
    public Optional<IdempotencyRecord> find(String key) {
        return Optional.ofNullable(store.get(key));
    }

    @Override
    public void save(IdempotencyRecord record) {
        store.putIfAbsent(record.getKey(), record);
    }
}
