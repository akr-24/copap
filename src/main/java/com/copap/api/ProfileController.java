package com.copap.api;

import com.copap.auth.AuthTokenRepository;
import com.copap.auth.JdbcUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ProfileController implements HttpHandler {

    private static final ObjectMapper mapper = new ObjectMapper();
    private final AuthTokenRepository authTokenRepository;
    private final Connection connection;

    public ProfileController(AuthTokenRepository authTokenRepository, Connection connection) {
        this.authTokenRepository = authTokenRepository;
        this.connection = connection;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        byte[] responseBytes = null;
        int statusCode = 200;

        try {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                statusCode = 405;
                responseBytes = "Method not allowed".getBytes();
                return;
            }

            // Get token from Authorization header
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                statusCode = 401;
                responseBytes = "Unauthorized".getBytes();
                return;
            }

            String token = authHeader.substring(7);
            var authToken = authTokenRepository.findByToken(token);
            if (authToken.isEmpty()) {
                statusCode = 401;
                responseBytes = "Unauthorized".getBytes();
                return;
            }

            String userId = authToken.get().getUserId();

            // Fetch user details
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT user_id, username, email FROM users WHERE user_id = ?"
            );
            stmt.setString(1, userId);
            var rs = stmt.executeQuery();

            if (!rs.next()) {
                statusCode = 404;
                responseBytes = "User not found".getBytes();
                return;
            }

            String responseJson = String.format(
                    "{ \"userId\": \"%s\", \"username\": \"%s\", \"email\": \"%s\" }",
                    rs.getString("user_id"),
                    rs.getString("username"),
                    rs.getString("email") != null ? rs.getString("email") : ""
            );

            responseBytes = responseJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");

        } catch (Exception e) {
            e.printStackTrace();
            statusCode = 500;
            responseBytes = ("{\"error\": \"" + e.getMessage() + "\"}").getBytes();
        } finally {
            exchange.sendResponseHeaders(statusCode, responseBytes != null ? responseBytes.length : 0);
            if (responseBytes != null) {
                exchange.getResponseBody().write(responseBytes);
            }
            exchange.close();
        }
    }
}

