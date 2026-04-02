package com.copap.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
public class JdbcUserRepository implements UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<User> USER_ROW_MAPPER = (rs, rowNum) -> new User(
            rs.getString("user_id"),
            rs.getString("username"),
            rs.getString("password_hash"),
            rs.getString("email"),
            rs.getString("role"),
            rs.getString("password_algo")
    );

    @Override
    public Optional<User> findByUsername(String username) {
        var results = jdbcTemplate.query(
                "SELECT user_id, username, password_hash, email, role, password_algo FROM users WHERE username = ?",
                USER_ROW_MAPPER,
                username
        );
        return results.stream().findFirst();
    }

    @Override
    public Optional<User> findById(String userId) {
        var results = jdbcTemplate.query(
                "SELECT user_id, username, password_hash, email, role, password_algo FROM users WHERE user_id = ?",
                USER_ROW_MAPPER,
                userId
        );
        return results.stream().findFirst();
    }

    @Override
    public void save(String userId, String username, String passwordHash, String email, String role, String passwordAlgo) {
        jdbcTemplate.update(
                "INSERT INTO users (user_id, username, password_hash, email, role, password_algo) VALUES (?, ?, ?, ?, ?, ?)",
                userId, username, passwordHash, email,
                role != null ? role : "USER",
                passwordAlgo != null ? passwordAlgo : "sha256"
        );
    }

    public void upgradePassword(String userId, String newBcryptHash) {
        jdbcTemplate.update(
                "UPDATE users SET password_hash = ?, password_algo = 'bcrypt' WHERE user_id = ?",
                newBcryptHash, userId
        );
    }
}
