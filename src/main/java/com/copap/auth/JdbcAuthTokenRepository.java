package com.copap.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

@Repository
public class JdbcAuthTokenRepository implements AuthTokenRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAuthTokenRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(AuthToken token) {
        jdbcTemplate.update(
                "INSERT INTO auth_tokens (token, user_id, expires_at) VALUES (?, ?, ?)",
                token.getToken(),
                token.getUserId(),
                Timestamp.from(token.getExpiresAt())
        );
    }

    @Override
    public Optional<AuthToken> findByToken(String tokenValue) {
        var results = jdbcTemplate.query(
                "SELECT token, user_id, expires_at FROM auth_tokens WHERE token = ?",
                (rs, rowNum) -> new AuthToken(
                        rs.getString("token"),
                        rs.getString("user_id"),
                        rs.getTimestamp("expires_at").toInstant()
                ),
                tokenValue
        );
        return results.stream().findFirst();
    }
}
