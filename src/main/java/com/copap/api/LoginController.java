package com.copap.api;

import com.copap.auth.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class LoginController implements HttpHandler {

    private static final ObjectMapper mapper = new ObjectMapper();
    private final AuthService authService;

    public LoginController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        LoginRequest req = mapper.readValue(
                exchange.getRequestBody(),
                LoginRequest.class
        );

        String token = authService.login(req.username, req.password);

        String response = """
            { "token": "%s" }
            """.formatted(token);

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    static class LoginRequest {
        public String username;
        public String password;
    }
}