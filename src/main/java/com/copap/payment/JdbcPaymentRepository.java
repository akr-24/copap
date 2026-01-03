package com.copap.payment;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcPaymentRepository implements PaymentRepository{

    private final Connection connection;
    public JdbcPaymentRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void save(Payment payment) {
        try {
            PreparedStatement stmt =
                    connection.prepareStatement(
                            """
                            INSERT INTO payments
                            (payment_id, order_id, amount, status, created_at)
                            VALUES (?, ?, ?, ?, ?)
                            """
                    );

            stmt.setString(1, payment.getPaymentId());
            stmt.setString(2, payment.getOrderId());
            stmt.setDouble(3, payment.getAmount());
            stmt.setString(4, payment.getStatus().name());
            stmt.setTimestamp(5, Timestamp.from(Instant.now()));

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Payment> findByOrderId(String orderId) {
        try {
            PreparedStatement stmt =
                    connection.prepareStatement(
                            "SELECT * FROM payments WHERE order_id = ?"
                    );

            stmt.setString(1, orderId);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) return Optional.empty();

            Payment payment = new Payment(
                    rs.getString("payment_id"),
                    rs.getString("order_id"),
                    rs.getDouble("amount")
            );

            payment.setStatus(
                    PaymentStatus.valueOf(rs.getString("status"))
            );

            return Optional.of(payment);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Payment> findIncompletePayments() {
        List<Payment> payments = new ArrayList<>();

        try {
            PreparedStatement stmt =
                    connection.prepareStatement(
                            """
                            SELECT * FROM payments
                            WHERE status IN (?, ?)
                            FOR UPDATE SKIP LOCKED
                            """
                    );

            stmt.setString(1, PaymentStatus.INITIATED.name());
            stmt.setString(2, PaymentStatus.PROCESSING.name());

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Payment payment = new Payment(
                        rs.getString("payment_id"),
                        rs.getString("order_id"),
                        rs.getDouble("amount")
                );

                payment.setStatus(
                        PaymentStatus.valueOf(rs.getString("status"))
                );

                payments.add(payment);
            }

            return payments;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void update(Payment payment) {
        try {
            PreparedStatement stmt =
                    connection.prepareStatement(
                            """
                            UPDATE payments
                            SET status = ?
                            WHERE payment_id = ?
                            """
                    );

            stmt.setString(1, payment.getStatus().name());
            stmt.setString(2, payment.getPaymentId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}