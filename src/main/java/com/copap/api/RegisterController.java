package com.copap.api;

import com.copap.auth.AuthService;
import com.copap.auth.PasswordHasher;
import com.copap.auth.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class RegisterController implements HttpHandler {

    private static final ObjectMapper mapper = new ObjectMapper();
    private final UserRepository userRepository;
    private final AuthService authService;

    public RegisterController(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        byte[] responseBytes = null;
        int statusCode = 200;

        try {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                statusCode = 405;
                responseBytes = "Method not allowed".getBytes();
                return;
            }

            String body = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            RegisterRequest req = mapper.readValue(body, RegisterRequest.class);

            // Check if username already exists
            if (userRepository.findByUsername(req.username).isPresent()) {
                statusCode = 400;
                responseBytes = "{\"error\": \"Username already exists\"}".getBytes();
                return;
            }

            // Create new user
            String userId = UUID.randomUUID().toString();
            String passwordHash = PasswordHasher.hash(req.password);

            // Save user (assuming JdbcUserRepository has a save method)
            if (userRepository instanceof com.copap.auth.JdbcUserRepository) {
                ((com.copap.auth.JdbcUserRepository) userRepository).save(userId, req.username, passwordHash, req.email);
            }

            // Auto-login after registration
            String token = authService.login(req.username, req.password);

            String responseJson = "{ \"token\": \"" + token + "\" }";
            responseBytes = responseJson.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().add("Content-Type", "application/json");

        } catch (Exception e) {
            e.printStackTrace();
            statusCode = 400;
            responseBytes = ("{\"error\": \"" + e.getMessage() + "\"}").getBytes();
        } finally {
            exchange.sendResponseHeaders(statusCode, responseBytes != null ? responseBytes.length : 0);
            if (responseBytes != null) {
                exchange.getResponseBody().write(responseBytes);
            }
            exchange.close();
        }
    }

    static class RegisterRequest {
        public String username;
        public String password;
        public String email;
    }
}

