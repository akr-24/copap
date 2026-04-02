package com.copap.payment;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcPaymentRepository implements PaymentRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<Payment> PAYMENT_ROW_MAPPER = (rs, rowNum) -> {
        Payment p = new Payment(
                rs.getString("payment_id"),
                rs.getString("order_id"),
                rs.getDouble("amount")
        );
        p.setStatus(PaymentStatus.valueOf(rs.getString("status")));
        return p;
    };

    public JdbcPaymentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(Payment payment) {
        jdbcTemplate.update(
                "INSERT INTO payments (payment_id, order_id, amount, status, created_at) VALUES (?, ?, ?, ?, ?)",
                payment.getPaymentId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getStatus().name(),
                Timestamp.from(Instant.now())
        );
    }

    @Override
    public Optional<Payment> findByOrderId(String orderId) {
        var results = jdbcTemplate.query(
                "SELECT payment_id, order_id, amount, status FROM payments WHERE order_id = ?",
                PAYMENT_ROW_MAPPER,
                orderId
        );
        return results.stream().findFirst();
    }

    @Override
    @Transactional
    public List<Payment> findIncompletePayments() {
        return jdbcTemplate.query("""
                SELECT payment_id, order_id, amount, status FROM payments
                WHERE status IN (?, ?)
                FOR UPDATE SKIP LOCKED
                """,
                PAYMENT_ROW_MAPPER,
                PaymentStatus.INITIATED.name(),
                PaymentStatus.PROCESSING.name()
        );
    }

    @Override
    public void update(Payment payment) {
        jdbcTemplate.update(
                "UPDATE payments SET status = ? WHERE payment_id = ?",
                payment.getStatus().name(),
                payment.getPaymentId()
        );
    }
}
