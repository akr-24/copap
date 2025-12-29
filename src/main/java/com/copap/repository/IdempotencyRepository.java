package com.copap.repository;

import com.copap.model.IdempotencyRecord;

import java.util.Optional;

public interface IdempotencyRepository {

    Optional<IdempotencyRecord> find(String key);

    void save(IdempotencyRecord record);
}
