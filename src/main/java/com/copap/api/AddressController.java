package com.copap.api;

import com.copap.auth.AuthTokenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

public class AddressController implements HttpHandler {

    private static final ObjectMapper mapper = new ObjectMapper();
    private final AuthTokenRepository authTokenRepository;
    private final Connection connection;

    public AddressController(AuthTokenRepository authTokenRepository, Connection connection) {
        this.authTokenRepository = authTokenRepository;
        this.connection = connection;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        System.out.println("AddressController hit - Method: " + method + ", Path: " + path);

        try {
            // Get user ID from auth token
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                sendError(exchange, 401, "Unauthorized");
                return;
            }

            String token = authHeader.substring(7);
            var authToken = authTokenRepository.findByToken(token);
            if (authToken.isEmpty()) {
                sendError(exchange, 401, "Unauthorized");
                return;
            }

            String userId = authToken.get().getUserId();

            // Route handling
            if (path.equals("/addresses") && method.equalsIgnoreCase("GET")) {
                handleGetAddresses(exchange, userId);
            } else if (path.equals("/addresses") && method.equalsIgnoreCase("POST")) {
                handleCreateAddress(exchange, userId);
            } else if (path.startsWith("/addresses/") && method.equalsIgnoreCase("PUT")) {
                handleUpdateAddress(exchange, userId);
            } else if (path.startsWith("/addresses/") && method.equalsIgnoreCase("DELETE")) {
                handleDeleteAddress(exchange, userId);
            } else if (path.startsWith("/addresses/") && path.endsWith("/default") && method.equalsIgnoreCase("POST")) {
                handleSetDefaultAddress(exchange, userId);
            } else {
                sendError(exchange, 404, "Not found");
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, e.getMessage());
        } finally {
            exchange.close();
        }
    }

    private void handleGetAddresses(HttpExchange exchange, String userId) throws IOException {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT address_id, label, full_name, phone, street, city, state, postal_code, country, is_default " +
                "FROM addresses WHERE user_id = ? ORDER BY is_default DESC, created_at DESC"
            );
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();

            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                first = false;
                json.append(String.format("""
                    {
                        "addressId": "%s",
                        "label": "%s",
                        "fullName": "%s",
                        "phone": "%s",
                        "street": "%s",
                        "city": "%s",
                        "state": "%s",
                        "postalCode": "%s",
                        "country": "%s",
                        "isDefault": %b
                    }
                    """,
                    rs.getString("address_id"),
                    rs.getString("label") != null ? rs.getString("label") : "",
                    rs.getString("full_name"),
                    rs.getString("phone") != null ? rs.getString("phone") : "",
                    rs.getString("street"),
                    rs.getString("city"),
                    rs.getString("state") != null ? rs.getString("state") : "",
                    rs.getString("postal_code"),
                    rs.getString("country") != null ? rs.getString("country") : "India",
                    rs.getBoolean("is_default")
                ));
            }
            json.append("]");

            sendJson(exchange, 200, json.toString());

        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void handleCreateAddress(HttpExchange exchange, String userId) throws IOException {
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            var request = mapper.readTree(body);

            String addressId = UUID.randomUUID().toString();
            String label = request.has("label") ? request.get("label").asText() : "Home";
            String fullName = request.get("fullName").asText();
            String phone = request.has("phone") ? request.get("phone").asText() : "";
            String street = request.get("street").asText();
            String city = request.get("city").asText();
            String state = request.has("state") ? request.get("state").asText() : "";
            String postalCode = request.get("postalCode").asText();
            String country = request.has("country") ? request.get("country").asText() : "India";
            boolean isDefault = request.has("isDefault") && request.get("isDefault").asBoolean();

            // If this is the default, unset other defaults
            if (isDefault) {
                PreparedStatement unsetStmt = connection.prepareStatement(
                    "UPDATE addresses SET is_default = false WHERE user_id = ?"
                );
                unsetStmt.setString(1, userId);
                unsetStmt.executeUpdate();
            }

            PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO addresses (address_id, user_id, label, full_name, phone, street, city, state, postal_code, country, is_default, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())"
            );
            stmt.setString(1, addressId);
            stmt.setString(2, userId);
            stmt.setString(3, label);
            stmt.setString(4, fullName);
            stmt.setString(5, phone);
            stmt.setString(6, street);
            stmt.setString(7, city);
            stmt.setString(8, state);
            stmt.setString(9, postalCode);
            stmt.setString(10, country);
            stmt.setBoolean(11, isDefault);
            stmt.executeUpdate();

            String json = String.format("""
                {
                    "addressId": "%s",
                    "label": "%s",
                    "fullName": "%s",
                    "phone": "%s",
                    "street": "%s",
                    "city": "%s",
                    "state": "%s",
                    "postalCode": "%s",
                    "country": "%s",
                    "isDefault": %b
                }
                """, addressId, label, fullName, phone, street, city, state, postalCode, country, isDefault);

            sendJson(exchange, 201, json);

        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void handleUpdateAddress(HttpExchange exchange, String userId) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String addressId = path.substring("/addresses/".length());

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            var request = mapper.readTree(body);

            // Build dynamic update query
            StringBuilder sql = new StringBuilder("UPDATE addresses SET ");
            boolean hasUpdate = false;

            if (request.has("label")) {
                sql.append("label = '").append(request.get("label").asText()).append("'");
                hasUpdate = true;
            }
            if (request.has("fullName")) {
                if (hasUpdate) sql.append(", ");
                sql.append("full_name = '").append(request.get("fullName").asText()).append("'");
                hasUpdate = true;
            }
            if (request.has("phone")) {
                if (hasUpdate) sql.append(", ");
                sql.append("phone = '").append(request.get("phone").asText()).append("'");
                hasUpdate = true;
            }
            if (request.has("street")) {
                if (hasUpdate) sql.append(", ");
                sql.append("street = '").append(request.get("street").asText()).append("'");
                hasUpdate = true;
            }
            if (request.has("city")) {
                if (hasUpdate) sql.append(", ");
                sql.append("city = '").append(request.get("city").asText()).append("'");
                hasUpdate = true;
            }
            if (request.has("state")) {
                if (hasUpdate) sql.append(", ");
                sql.append("state = '").append(request.get("state").asText()).append("'");
                hasUpdate = true;
            }
            if (request.has("postalCode")) {
                if (hasUpdate) sql.append(", ");
                sql.append("postal_code = '").append(request.get("postalCode").asText()).append("'");
                hasUpdate = true;
            }
            if (request.has("country")) {
                if (hasUpdate) sql.append(", ");
                sql.append("country = '").append(request.get("country").asText()).append("'");
                hasUpdate = true;
            }

            sql.append(" WHERE address_id = '").append(addressId).append("' AND user_id = '").append(userId).append("'");

            if (hasUpdate) {
                connection.createStatement().executeUpdate(sql.toString());
            }

            sendJson(exchange, 200, "{\"success\": true}");

        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void handleDeleteAddress(HttpExchange exchange, String userId) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String addressId = path.substring("/addresses/".length());

            PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM addresses WHERE address_id = ? AND user_id = ?"
            );
            stmt.setString(1, addressId);
            stmt.setString(2, userId);
            stmt.executeUpdate();

            sendJson(exchange, 200, "{\"success\": true}");

        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void handleSetDefaultAddress(HttpExchange exchange, String userId) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String addressId = path.substring("/addresses/".length()).replace("/default", "");

            // Unset all defaults
            PreparedStatement unsetStmt = connection.prepareStatement(
                "UPDATE addresses SET is_default = false WHERE user_id = ?"
            );
            unsetStmt.setString(1, userId);
            unsetStmt.executeUpdate();

            // Set new default
            PreparedStatement setStmt = connection.prepareStatement(
                "UPDATE addresses SET is_default = true WHERE address_id = ? AND user_id = ?"
            );
            setStmt.setString(1, addressId);
            setStmt.setString(2, userId);
            setStmt.executeUpdate();

            sendJson(exchange, 200, "{\"success\": true}");

        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        String json = String.format("{\"error\": \"%s\"}", message);
        sendJson(exchange, status, json);
    }
}

