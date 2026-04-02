package com.copap.product;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcProductRepository implements ProductRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<Product> PRODUCT_ROW_MAPPER = (rs, rowNum) -> new Product(
            rs.getString("product_id"),
            rs.getString("name"),
            rs.getDouble("price"),
            rs.getBoolean("active"),
            rs.getString("image_filename")
    );

    public JdbcProductRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Product> findById(String productId) {
        var results = jdbcTemplate.query(
                "SELECT product_id, name, price, active, image_filename FROM products WHERE product_id = ? AND active = true",
                PRODUCT_ROW_MAPPER, productId
        );
        return results.stream().findFirst();
    }

    @Override
    public List<Product> findByIds(List<String> productIds) {
        if (productIds.isEmpty()) return List.of();
        String placeholders = String.join(",", Collections.nCopies(productIds.size(), "?"));
        return jdbcTemplate.query(
                "SELECT product_id, name, price, active, image_filename FROM products WHERE product_id IN (" + placeholders + ") AND active = true",
                PRODUCT_ROW_MAPPER, productIds.toArray()
        );
    }

    @Override
    public List<Product> findAll() {
        return jdbcTemplate.query(
                "SELECT product_id, name, price, active, image_filename FROM products",
                PRODUCT_ROW_MAPPER
        );
    }

    public void save(String productId, String name, double price) {
        jdbcTemplate.update(
                "INSERT INTO products (product_id, name, price, active) VALUES (?, ?, ?, true)",
                productId, name, price
        );
    }

    public void update(String productId, String name, Double price, Boolean active) {
        if (name != null) {
            jdbcTemplate.update("UPDATE products SET name = ? WHERE product_id = ?", name, productId);
        }
        if (price != null) {
            jdbcTemplate.update("UPDATE products SET price = ? WHERE product_id = ?", price, productId);
        }
        if (active != null) {
            jdbcTemplate.update("UPDATE products SET active = ? WHERE product_id = ?", active, productId);
        }
    }

    public void updateImageFilename(String productId, String filename) {
        jdbcTemplate.update(
                "UPDATE products SET image_filename = ? WHERE product_id = ?",
                filename, productId
        );
    }

    public void softDelete(String productId) {
        jdbcTemplate.update("UPDATE products SET active = false WHERE product_id = ?", productId);
    }
}
