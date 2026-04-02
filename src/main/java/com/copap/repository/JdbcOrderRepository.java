package com.copap.repository;

import com.copap.model.Order;
import com.copap.model.OrderStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcOrderRepository implements OrderRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcOrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void save(Order order) {
        jdbcTemplate.update("""
                INSERT INTO orders
                    (order_id, status, customer_id, total_amount, version, created_at, shipping_address_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (order_id) DO UPDATE
                    SET status = EXCLUDED.status,
                        version = EXCLUDED.version,
                        shipping_address_id = COALESCE(EXCLUDED.shipping_address_id, orders.shipping_address_id)
                """,
                order.getOrderId(),
                order.getStatus().name(),
                order.getCustomerId(),
                order.totalAmount(),
                order.getVersion(),
                Timestamp.from(Instant.now()),
                order.getShippingAddressId()
        );

        // Persist order-product relationships in order_items
        if (!order.getProductIds().isEmpty()) {
            jdbcTemplate.update(
                    "DELETE FROM order_items WHERE order_id = ?",
                    order.getOrderId()
            );
            for (String productId : order.getProductIds()) {
                jdbcTemplate.update(
                        "INSERT INTO order_items (order_id, product_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                        order.getOrderId(), productId
                );
            }
        }
    }

    @Override
    public Optional<Order> findById(String orderId) {
        var rows = jdbcTemplate.query(
                "SELECT order_id, status, customer_id, total_amount, version, shipping_address_id FROM orders WHERE order_id = ?",
                (rs, rowNum) -> {
                    List<String> productIds = jdbcTemplate.queryForList(
                            "SELECT product_id FROM order_items WHERE order_id = ?",
                            String.class,
                            orderId
                    );
                    return Order.fromDb(
                            rs.getString("order_id"),
                            OrderStatus.valueOf(rs.getString("status")),
                            rs.getString("customer_id"),
                            productIds,
                            rs.getDouble("total_amount"),
                            rs.getLong("version"),
                            rs.getString("shipping_address_id")
                    );
                },
                orderId
        );
        return rows.stream().findFirst();
    }

    @Override
    public boolean exists(String orderId) {
        var count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE order_id = ?",
                Long.class,
                orderId
        );
        return count != null && count > 0;
    }

    @Override
    public void updateWithVersion(Order order, long expectedVersion) {
        int updated = jdbcTemplate.update("""
                UPDATE orders
                SET status = ?, version = version + 1
                WHERE order_id = ? AND version = ?
                """,
                order.getStatus().name(),
                order.getOrderId(),
                expectedVersion
        );

        if (updated == 0) {
            throw new OptimisticLockException(
                    "Version mismatch for order " + order.getOrderId()
            );
        }
    }

    @Override
    public List<Order> findByCustomerId(String customerId) {
        return jdbcTemplate.query(
                "SELECT order_id, status, customer_id, total_amount, version, shipping_address_id FROM orders WHERE customer_id = ? ORDER BY created_at DESC",
                (rs, rowNum) -> {
                    String orderId = rs.getString("order_id");
                    List<String> productIds = jdbcTemplate.queryForList(
                            "SELECT product_id FROM order_items WHERE order_id = ?",
                            String.class,
                            orderId
                    );
                    return Order.fromDb(
                            orderId,
                            OrderStatus.valueOf(rs.getString("status")),
                            rs.getString("customer_id"),
                            productIds,
                            rs.getDouble("total_amount"),
                            rs.getLong("version"),
                            rs.getString("shipping_address_id")
                    );
                },
                customerId
        );
    }
}
