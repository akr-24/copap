package com.copap.repository;

import com.copap.model.IdempotencyRecord;

import java.sql.*;
import java.time.Instant;

public class JdbcIdempotencyRepository
        implements IdempotencyRepository {

    private final Connection connection;

    public JdbcIdempotencyRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public IdempotencyRecord saveOrGet(
            String key,
            String hash,
            String orderId) {

        try {
            PreparedStatement insert =
                    connection.prepareStatement(
                            """
                            INSERT INTO idempotency_records
                            (idempotency_key, request_hash, order_id, created_at)
                            VALUES (?, ?, ?, ?)
                            ON CONFLICT (idempotency_key)
                            DO NOTHING
                            """
                    );

            insert.setString(1, key);
            insert.setString(2, hash);
            insert.setString(3, orderId);
            insert.setTimestamp(4, Timestamp.from(Instant.now()));

            insert.executeUpdate();

            PreparedStatement select =
                    connection.prepareStatement(
                            "SELECT * FROM idempotency_records WHERE idempotency_key = ?"
                    );

            select.setString(1, key);
            ResultSet rs = select.executeQuery();

            if (!rs.next()) {
                throw new IllegalStateException("Idempotency record missing");
            }

            if (!rs.getString("request_hash").equals(hash)) {
                throw new IllegalStateException(
                        "Idempotency key reused with different request"
                );
            }

            return new IdempotencyRecord(
                    rs.getString("idempotency_key"),
                    rs.getString("order_id"),
                    rs.getString("request_hash"),
                    rs.getTimestamp("created_at").toInstant()
            );

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
