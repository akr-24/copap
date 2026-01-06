package com.copap.repository;

import com.copap.model.Order;
import com.copap.model.OrderStatus;

import java.sql.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class JdbcOrderRepository implements OrderRepository {

    private final Connection connection;

    public JdbcOrderRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void save(Order order) {
        try{
            PreparedStatement stmt =
                    connection.prepareStatement(
                            """
                            INSERT INTO orders
                            (order_id, status, customer_id, total_amount, version, created_at, shipping_address_id)
                            VALUES (?, ?, ?, ?, ?, ?, ?)
                            ON CONFLICT (order_id) DO UPDATE
                            SET status = EXCLUDED.status,
                                version = EXCLUDED.version,
                                shipping_address_id = COALESCE(EXCLUDED.shipping_address_id, orders.shipping_address_id)
                            """
                    );

            stmt.setString(1, order.getOrderId());
            stmt.setString(2, order.getStatus().name());
            stmt.setString(3, order.getCustomerId());
            stmt.setDouble(4, order.totalAmount());
            stmt.setLong(5, order.getVersion());
            stmt.setTimestamp(6, Timestamp.from(Instant.now()));
            stmt.setString(7, order.getShippingAddressId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Order> findById (String orderId) {
        try{
           PreparedStatement stmt = connection.prepareStatement(" Select * from orders where order_id=?");
           stmt.setString(1, orderId);
           ResultSet rs = stmt.executeQuery();
           if (!rs.next()) return Optional.empty();
            Order order = Order.fromDb(
                    rs.getString("order_id"),                       // orderId
                    OrderStatus.valueOf(rs.getString("status")),    // status
                    rs.getString("customer_id"),                    // customerId
                    List.of(),                                      // productIds (TEMP)
                    rs.getDouble("total_amount"),
                    rs.getLong("version"),                          // version
                    rs.getString("shipping_address_id")             // shippingAddressId
            );

            return Optional.of(order);
        }catch(SQLException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean exists(String orderId) {
        return findById(orderId).isPresent();
    }

    @Override
    public void updateWithVersion(Order order, long expectedVersion) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    """
                    UPDATE orders
                    SET status = ?, version = version + 1
                    WHERE order_id = ? AND version = ?
                    """
            );

            stmt.setString(1, order.getStatus().name());
            stmt.setString(2, order.getOrderId());
            stmt.setLong(3, expectedVersion);

            int updated = stmt.executeUpdate();

            if (updated == 0) {
                throw new OptimisticLockException(
                        "Version mismatch for order " + order.getOrderId()
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Order> findByCustomerId(String customerId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT * FROM orders WHERE customer_id = ? ORDER BY created_at DESC"
            );
            stmt.setString(1, customerId);
            ResultSet rs = stmt.executeQuery();

            List<Order> orders = new java.util.ArrayList<>();
            while (rs.next()) {
                Order order = Order.fromDb(
                        rs.getString("order_id"),
                        OrderStatus.valueOf(rs.getString("status")),
                        rs.getString("customer_id"),
                        List.of(), // productIds - would need separate query for order_items
                        rs.getDouble("total_amount"),
                        rs.getLong("version"),
                        rs.getString("shipping_address_id")
                );
                orders.add(order);
            }
            return orders;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}