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
                    "Select user_id, username, password_hash from users where username = ?"
            );
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if(!rs.next()){ return Optional.empty(); }

            return Optional.of(
                    new User(rs.getString("user_id"),
                            rs.getString("username"),
                            rs.getString("password_hash")
                    ));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}