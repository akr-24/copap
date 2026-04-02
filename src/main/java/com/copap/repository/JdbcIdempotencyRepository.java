package com.copap.repository;

import com.copap.model.IdempotencyRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
public class JdbcIdempotencyRepository implements IdempotencyRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcIdempotencyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public IdempotencyRecord saveOrGet(String key, String hash, String orderId) {
        jdbcTemplate.update("""
                INSERT INTO idempotency_records (idempotency_key, request_hash, order_id, created_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (idempotency_key) DO NOTHING
                """,
                key, hash, orderId, Timestamp.from(Instant.now())
        );

        var results = jdbcTemplate.query(
                "SELECT idempotency_key, request_hash, order_id, created_at FROM idempotency_records WHERE idempotency_key = ?",
                (rs, rowNum) -> new IdempotencyRecord(
                        rs.getString("idempotency_key"),
                        rs.getString("order_id"),
                        rs.getString("request_hash"),
                        rs.getTimestamp("created_at").toInstant()
                ),
                key
        );

        if (results.isEmpty()) {
            throw new IllegalStateException("Idempotency record missing after insert");
        }

        IdempotencyRecord record = results.get(0);

        if (!record.getRequestHash().equals(hash)) {
            throw new IllegalStateException(
                    "Idempotency key '" + key + "' reused with a different request payload"
            );
        }

        return record;
    }
}
