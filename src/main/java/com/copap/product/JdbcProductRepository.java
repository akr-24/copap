package com.copap.product;

import java.sql.*;
import java.util.*;

public class JdbcProductRepository implements ProductRepository {

    private final Connection connection;

    public JdbcProductRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<Product> findById(String productId) {
        try {
            PreparedStatement stmt =
                    connection.prepareStatement(
                            "SELECT * FROM products WHERE product_id = ? AND active = true"
                    );

            stmt.setString(1, productId);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) return Optional.empty();

            return Optional.of(mapRow(rs));

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Product> findByIds(List<String> productIds) {
        if (productIds.isEmpty()) return List.of();

        String placeholders =
                String.join(",", Collections.nCopies(productIds.size(), "?"));

        String sql =
                "SELECT * FROM products WHERE product_id IN (" +
                        placeholders +
                        ") AND active = true";

        try {
            PreparedStatement stmt =
                    connection.prepareStatement(sql);

            for (int i = 0; i < productIds.size(); i++) {
                stmt.setString(i + 1, productIds.get(i));
            }

            ResultSet rs = stmt.executeQuery();

            List<Product> products = new ArrayList<>();

            while (rs.next()) {
                products.add(mapRow(rs));
            }

            return products;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        return new Product(
                rs.getString("product_id"),
                rs.getString("name"),
                rs.getDouble("price"),
                rs.getBoolean("active")
        );
    }
}
