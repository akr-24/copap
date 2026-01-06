package com.copap.api;

import com.copap.analytics.SlidingWindowAnalytics;
import com.copap.auth.AuthTokenRepository;
import com.copap.auth.JdbcUserRepository;
import com.copap.auth.User;
import com.copap.product.Product;
import com.copap.product.ProductRepository;
import com.copap.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

public class AdminController implements HttpHandler {

    private static final ObjectMapper mapper = new ObjectMapper();
    private final AuthTokenRepository authTokenRepository;
    private final JdbcUserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final SlidingWindowAnalytics analytics;
    private final Connection connection;

    public AdminController(AuthTokenRepository authTokenRepository,
                          JdbcUserRepository userRepository,
                          ProductRepository productRepository,
                          OrderRepository orderRepository,
                          SlidingWindowAnalytics analytics,
                          Connection connection) {
        this.authTokenRepository = authTokenRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.analytics = analytics;
        this.connection = connection;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        System.out.println("AdminController hit - Method: " + method + ", Path: " + path);

        try {
            // Verify admin access
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
            var userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty() || !isAdmin(userId)) {
                sendError(exchange, 403, "Admin access required");
                return;
            }

            // Route handling
            if (path.equals("/admin/analytics") && method.equalsIgnoreCase("GET")) {
                handleGetAnalytics(exchange);
            } else if (path.equals("/admin/dashboard") && method.equalsIgnoreCase("GET")) {
                handleGetDashboard(exchange);
            } else if (path.equals("/admin/products") && method.equalsIgnoreCase("GET")) {
                handleGetProducts(exchange);
            } else if (path.equals("/admin/products") && method.equalsIgnoreCase("POST")) {
                handleCreateProduct(exchange);
            } else if (path.startsWith("/admin/products/") && method.equalsIgnoreCase("PUT")) {
                handleUpdateProduct(exchange);
            } else if (path.startsWith("/admin/products/") && method.equalsIgnoreCase("DELETE")) {
                handleDeleteProduct(exchange);
            } else if (path.equals("/admin/orders") && method.equalsIgnoreCase("GET")) {
                handleGetAllOrders(exchange);
            } else if (path.equals("/admin/users") && method.equalsIgnoreCase("GET")) {
                handleGetUsers(exchange);
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

    private boolean isAdmin(String userId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT role FROM users WHERE user_id = ?"
            );
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String role = rs.getString("role");
                return "ADMIN".equalsIgnoreCase(role);
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void handleGetAnalytics(HttpExchange exchange) throws IOException {
        long orderCount = analytics.getOrderCount();
        double revenue = analytics.getRevenue();

        String json = String.format("""
            {
                "windowOrderCount": %d,
                "windowRevenue": %.2f,
                "timestamp": "%s"
            }
            """, orderCount, revenue, java.time.Instant.now());

        sendJson(exchange, 200, json);
    }

    private void handleGetDashboard(HttpExchange exchange) throws IOException {
        try {
            // Get total orders
            PreparedStatement orderStmt = connection.prepareStatement("SELECT COUNT(*) as total FROM orders");
            ResultSet orderRs = orderStmt.executeQuery();
            long totalOrders = orderRs.next() ? orderRs.getLong("total") : 0;

            // Get total revenue
            PreparedStatement revenueStmt = connection.prepareStatement(
                "SELECT COALESCE(SUM(total_amount), 0) as total FROM orders WHERE status = 'PAID'"
            );
            ResultSet revenueRs = revenueStmt.executeQuery();
            double totalRevenue = revenueRs.next() ? revenueRs.getDouble("total") : 0;

            // Get total users
            PreparedStatement userStmt = connection.prepareStatement("SELECT COUNT(*) as total FROM users");
            ResultSet userRs = userStmt.executeQuery();
            long totalUsers = userRs.next() ? userRs.getLong("total") : 0;

            // Get total products
            PreparedStatement productStmt = connection.prepareStatement("SELECT COUNT(*) as total FROM products");
            ResultSet productRs = productStmt.executeQuery();
            long totalProducts = productRs.next() ? productRs.getLong("total") : 0;

            // Get orders by status
            PreparedStatement statusStmt = connection.prepareStatement(
                "SELECT status, COUNT(*) as count FROM orders GROUP BY status"
            );
            ResultSet statusRs = statusStmt.executeQuery();
            StringBuilder statusJson = new StringBuilder();
            while (statusRs.next()) {
                if (statusJson.length() > 0) statusJson.append(",");
                statusJson.append(String.format("\"%s\": %d", 
                    statusRs.getString("status"), 
                    statusRs.getLong("count")));
            }

            // Get recent orders
            PreparedStatement recentStmt = connection.prepareStatement(
                "SELECT order_id, status, total_amount, created_at FROM orders ORDER BY created_at DESC LIMIT 10"
            );
            ResultSet recentRs = recentStmt.executeQuery();
            StringBuilder recentJson = new StringBuilder("[");
            boolean first = true;
            while (recentRs.next()) {
                if (!first) recentJson.append(",");
                first = false;
                recentJson.append(String.format("""
                    {"orderId": "%s", "status": "%s", "totalAmount": %.2f, "createdAt": "%s"}
                    """,
                    recentRs.getString("order_id"),
                    recentRs.getString("status"),
                    recentRs.getDouble("total_amount"),
                    recentRs.getTimestamp("created_at")
                ));
            }
            recentJson.append("]");

            String json = String.format("""
                {
                    "totalOrders": %d,
                    "totalRevenue": %.2f,
                    "totalUsers": %d,
                    "totalProducts": %d,
                    "ordersByStatus": {%s},
                    "recentOrders": %s,
                    "windowAnalytics": {
                        "orderCount": %d,
                        "revenue": %.2f
                    }
                }
                """, 
                totalOrders, totalRevenue, totalUsers, totalProducts,
                statusJson.toString(), recentJson.toString(),
                analytics.getOrderCount(), analytics.getRevenue()
            );

            sendJson(exchange, 200, json);

        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void handleGetProducts(HttpExchange exchange) throws IOException {
        try {
            var products = productRepository.findAll();
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            for (var product : products) {
                if (!first) json.append(",");
                first = false;
                json.append(String.format("""
                    {"productId": "%s", "name": "%s", "price": %.2f, "active": %b}
                    """,
                    product.getProductId(),
                    product.getName(),
                    product.getPrice(),
                    product.isActive()
                ));
            }
            json.append("]");
            sendJson(exchange, 200, json.toString());
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void handleCreateProduct(HttpExchange exchange) throws IOException {
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            var request = mapper.readTree(body);
            
            String productId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String name = request.get("name").asText();
            double price = request.get("price").asDouble();

            PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO products (product_id, name, price, active) VALUES (?, ?, ?, true)"
            );
            stmt.setString(1, productId);
            stmt.setString(2, name);
            stmt.setDouble(3, price);
            stmt.executeUpdate();

            String json = String.format("""
                {"productId": "%s", "name": "%s", "price": %.2f, "active": true}
                """, productId, name, price);

            sendJson(exchange, 201, json);

        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void handleUpdateProduct(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String productId = path.substring("/admin/products/".length());
            
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            var request = mapper.readTree(body);
            
            String name = request.has("name") ? request.get("name").asText() : null;
            Double price = request.has("price") ? request.get("price").asDouble() : null;
            Boolean active = request.has("active") ? request.get("active").asBoolean() : null;

            StringBuilder sql = new StringBuilder("UPDATE products SET ");
            boolean hasUpdate = false;
            
            if (name != null) {
                sql.append("name = '").append(name).append("'");
                hasUpdate = true;
            }
            if (price != null) {
                if (hasUpdate) sql.append(", ");
                sql.append("price = ").append(price);
                hasUpdate = true;
            }
            if (active != null) {
                if (hasUpdate) sql.append(", ");
                sql.append("active = ").append(active);
                hasUpdate = true;
            }
            
            sql.append(" WHERE product_id = '").append(productId).append("'");

            if (hasUpdate) {
                connection.createStatement().executeUpdate(sql.toString());
            }

            sendJson(exchange, 200, "{\"success\": true}");

        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void handleDeleteProduct(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String productId = path.substring("/admin/products/".length());

            PreparedStatement stmt = connection.prepareStatement(
                "UPDATE products SET active = false WHERE product_id = ?"
            );
            stmt.setString(1, productId);
            stmt.executeUpdate();

            sendJson(exchange, 200, "{\"success\": true}");

        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void handleGetAllOrders(HttpExchange exchange) throws IOException {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT order_id, customer_id, status, total_amount, version, created_at FROM orders ORDER BY created_at DESC"
            );
            ResultSet rs = stmt.executeQuery();

            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                first = false;
                json.append(String.format("""
                    {"orderId": "%s", "customerId": "%s", "status": "%s", "totalAmount": %.2f, "version": %d, "createdAt": "%s"}
                    """,
                    rs.getString("order_id"),
                    rs.getString("customer_id"),
                    rs.getString("status"),
                    rs.getDouble("total_amount"),
                    rs.getLong("version"),
                    rs.getTimestamp("created_at")
                ));
            }
            json.append("]");

            sendJson(exchange, 200, json.toString());

        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void handleGetUsers(HttpExchange exchange) throws IOException {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT user_id, username, email, role FROM users"
            );
            ResultSet rs = stmt.executeQuery();

            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                first = false;
                json.append(String.format("""
                    {"userId": "%s", "username": "%s", "email": "%s", "role": "%s"}
                    """,
                    rs.getString("user_id"),
                    rs.getString("username"),
                    rs.getString("email") != null ? rs.getString("email") : "",
                    rs.getString("role") != null ? rs.getString("role") : "USER"
                ));
            }
            json.append("]");

            sendJson(exchange, 200, json.toString());

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

