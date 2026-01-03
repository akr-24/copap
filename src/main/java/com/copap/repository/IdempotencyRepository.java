package com.copap.repository;

import com.copap.model.IdempotencyRecord;

import java.util.Optional;

public interface IdempotencyRepository {

    IdempotencyRecord saveOrGet(
            String idempotencyKey,
            String requestHash,
            String orderId
    );

}
