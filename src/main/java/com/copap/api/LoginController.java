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

        System.out.println("LoginController hit");

        byte[] responseBytes = null;
        int statusCode = 200;

        try {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                statusCode = 405;
                return;
            }

            String body = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            System.out.println("Login body => " + body);

            LoginRequest req =
                    mapper.readValue(body, LoginRequest.class);

            String token =
                    authService.login(req.username, req.password);

            String responseJson =
                    "{ \"token\": \"" + token + "\" }";

            responseBytes =
                    responseJson.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                    .add("Content-Type", "application/json");

        } catch (Exception e) {
            e.printStackTrace();
            statusCode = 401;
            responseBytes = "Unauthorized".getBytes();
        } finally {
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
            exchange.close();
        }
    }

    static class LoginRequest {
        public String username;
        public String password;
    }
}