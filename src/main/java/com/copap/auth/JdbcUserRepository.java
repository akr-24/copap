package com.copap.auth;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class JdbcUserRepository implements UserRepository {

    private final Connection connection;

    public JdbcUserRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        try{
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT user_id, username, password_hash, email, role FROM users WHERE username = ?"
            );
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if(!rs.next()){ return Optional.empty(); }

            return Optional.of(
                    new User(rs.getString("user_id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("email"),
                            rs.getString("role")
                    ));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void save(String userId, String username, String passwordHash, String email) {
        save(userId, username, passwordHash, email, "USER");
    }

    public void save(String userId, String username, String passwordHash, String email, String role) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO users (user_id, username, password_hash, email, role) VALUES (?, ?, ?, ?, ?)"
            );
            stmt.setString(1, userId);
            stmt.setString(2, username);
            stmt.setString(3, passwordHash);
            stmt.setString(4, email);
            stmt.setString(5, role != null ? role : "USER");
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<User> findById(String userId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT user_id, username, password_hash, email, role FROM users WHERE user_id = ?"
            );
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                return Optional.empty();
            }
            return Optional.of(new User(
                    rs.getString("user_id"),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getString("email"),
                    rs.getString("role")
            ));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}