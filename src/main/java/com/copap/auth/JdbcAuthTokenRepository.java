package com.copap.auth;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.Optional;

public class JdbcAuthTokenRepository implements AuthTokenRepository {

    private final Connection connection;

    public JdbcAuthTokenRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void save(AuthToken token) {

        try {
            PreparedStatement stmt = connection.prepareStatement(
                    """
                    INSERT INTO auth_tokens (token, user_id, expires_at)
                    VALUES (?, ?, ?)
                    """
            );

            stmt.setString(1, token.getToken());
            stmt.setString(2, token.getUserId());
            stmt.setObject(3, token.getExpiresAt());

            stmt.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<AuthToken> findByToken(String tokenValue) {

        try {
            PreparedStatement stmt = connection.prepareStatement(
                    """
                    SELECT token, user_id, expires_at
                    FROM auth_tokens
                    WHERE token = ?
                    """
            );

            stmt.setString(1, tokenValue);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) return Optional.empty();

            return Optional.of(
                    new AuthToken(
                            rs.getString("token"),
                            rs.getString("user_id"),
                            rs.getObject("expires_at", Instant.class)
                    )
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}