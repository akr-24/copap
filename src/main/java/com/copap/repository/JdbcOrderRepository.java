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
                            (order_id, status, customer_id, total_amount, version, created_at)
                            VALUES (?, ?, ?, ?, ?, ?)
                            ON CONFLICT (order_id) DO UPDATE
                            SET status = EXCLUDED.status,
                                version = EXCLUDED.version
                            """
                    );

            stmt.setString(1, order.getOrderId());
            stmt.setString(2, order.getStatus().name());
            stmt.setString(3, order.getCustomerId());
            stmt.setDouble(4, order.totalAmount());
            stmt.setLong(5, order.getVersion());
            stmt.setTimestamp(6, Timestamp.from(Instant.now()));

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
                    rs.getLong("version")                        // version
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
}