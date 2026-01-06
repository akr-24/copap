package com.copap.api;

import com.copap.api.dto.CreateOrderRequest;
import com.copap.auth.AuthTokenRepository;
import com.copap.db.TransactionManager;
import com.copap.engine.FailureAwareExecutor;
import com.copap.model.Order;
import com.copap.model.OrderStatus;
import com.copap.payment.PaymentService;
import com.copap.product.ProductRepository;
import com.copap.repository.OrderRepository;
import com.copap.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OrderController implements HttpHandler {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PaymentService paymentService;
    private final TransactionManager transactionManager;
    private final FailureAwareExecutor executor;
    private final AuthTokenRepository authTokenRepository;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public OrderController(OrderService orderService,
                           OrderRepository orderRepository,
                           ProductRepository productRepository,
                           PaymentService paymentService,
                           TransactionManager transactionManager,
                           FailureAwareExecutor executor,
                           AuthTokenRepository authTokenRepository) {

        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.paymentService = paymentService;
        this.transactionManager = transactionManager;
        this.executor = executor;
        this.authTokenRepository = authTokenRepository;
    }

    private void handlePost(HttpExchange exchange) throws IOException {

    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        System.out.println("OrderController hit - Method: " + method + ", Path: " + path);

        try {
            // -------------------------
            // POST /orders  (Create)
            // -------------------------
            if (method.equalsIgnoreCase("POST") && path.equals("/orders")) {
                System.out.println("Handling POST /orders");
                handleCreateOrder(exchange);
                return;
            }

            // -------------------------
            // GET /orders (List all orders for customer)
            // -------------------------
            if (method.equalsIgnoreCase("GET") && path.equals("/orders")) {
                System.out.println("Handling GET /orders");
                handleListOrders(exchange);
                return;
            }

            // -------------------------
            // GET /orders/{orderId}
            // -------------------------
            if (method.equalsIgnoreCase("GET") && path.startsWith("/orders/")) {
                System.out.println("Handling GET /orders/{orderId} - Path: " + path);
                handleGetOrder(exchange);
                return;
            }

            // -------------------------
            // Unsupported route
            // -------------------------
            System.out.println("Unsupported route - Method: " + method + ", Path: " + path);
            exchange.sendResponseHeaders(404, -1);

        } catch (Exception e) {
            e.printStackTrace();
            exchange.sendResponseHeaders(500, -1);
        } finally {
            exchange.close();
        }
    }

    private void handleCreateOrder(HttpExchange exchange) throws IOException {

        String idempotencyKey =
                exchange.getRequestHeaders().getFirst("Idempotency-Key");

        if (idempotencyKey == null) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }

        // Get customer ID from auth token
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.sendResponseHeaders(401, -1);
            return;
        }

        String token = authHeader.substring(7);
        var authToken = authTokenRepository.findByToken(token);
        if (authToken.isEmpty()) {
            exchange.sendResponseHeaders(401, -1);
            return;
        }

        String customerId = authToken.get().getUserId();

        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        System.out.println("RAW BODY => " + body);

        CreateOrderRequest request =
                objectMapper.readValue(body, CreateOrderRequest.class);

        List<String> productIds = request.productIds;

        var products = productRepository.findByIds(productIds);

        double totalAmount =
                products.stream().mapToDouble(p -> p.getPrice()).sum();

        String requestHash =
                customerId + "|" + productIds + "|" + totalAmount;

        String addressId = request.addressId;

        Order order =
                orderService.createOrder(
                        idempotencyKey,
                        requestHash,
                        customerId,
                        productIds,
                        totalAmount,
                        addressId
                );

        // Transition order to PAYMENT_PENDING before processing payment
        orderService.advanceOrder(order.getOrderId(), OrderStatus.PAYMENT_PENDING);

        // Trigger async payment processing
        orderService.startPaymentProcessing(order.getOrderId());

        System.out.println("Order created - Order ID: " + order.getOrderId() + ", Status: " + order.getStatus());

        String response =
                """
                {
                  "orderId": "%s",
                  "status": "%s",
                  "totalAmount": %.2f
                }
                """.formatted(
                        order.getOrderId(),
                        order.getStatus(),
                        order.totalAmount()
                );

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private void handleListOrders(HttpExchange exchange) throws IOException {
        try {
            // Get customer ID from auth token
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                exchange.sendResponseHeaders(401, -1);
                return;
            }

            String token = authHeader.substring(7);
            var authToken = authTokenRepository.findByToken(token);
            if (authToken.isEmpty()) {
                exchange.sendResponseHeaders(401, -1);
                return;
            }

            String customerId = authToken.get().getUserId();
            List<Order> orders = orderRepository.findByCustomerId(customerId);

            String ordersJson = orders.stream()
                    .map(order -> String.format(
                            """
                            {
                              "orderId": "%s",
                              "status": "%s",
                              "totalAmount": %.2f,
                              "version": %d,
                              "createdAt": "%s"
                            }
                            """,
                            order.getOrderId(),
                            order.getStatus(),
                            order.totalAmount(),
                            order.getVersion(),
                            order.getCreatedAt()
                    ))
                    .collect(Collectors.joining(",", "[", "]"));

            byte[] bytes = ordersJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);

        } catch (Exception e) {
            e.printStackTrace();
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private void handleGetOrder(HttpExchange exchange) throws IOException {
        System.out.println("handleGetOrder called");
        try {
            // Get customer ID from auth token for security
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            System.out.println("Auth header: " + (authHeader != null ? "Present" : "Missing"));
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                System.out.println("No valid auth header, returning 401");
                exchange.sendResponseHeaders(401, -1);
                return;
            }

            String token = authHeader.substring(7);
            System.out.println("Token extracted, length: " + token.length());
            
            var authToken = authTokenRepository.findByToken(token);
            if (authToken.isEmpty()) {
                System.out.println("Token not found in repository");
                exchange.sendResponseHeaders(401, -1);
                return;
            }

            String customerId = authToken.get().getUserId();
            System.out.println("Customer ID from token: " + customerId);

            String path = exchange.getRequestURI().getPath();
            String orderId = path.substring("/orders/".length());
            
            System.out.println("Fetching order: " + orderId + " for customer: " + customerId);

            Optional<Order> orderOpt = orderService.getOrder(orderId);
            
            if (orderOpt.isEmpty()) {
                System.out.println("Order not found in database: " + orderId);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                String errorResponse = "{\"error\": \"Order not found\"}";
                byte[] bytes = errorResponse.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, bytes.length);
                exchange.getResponseBody().write(bytes);
                return;
            }

            Order order = orderOpt.get();
            
            System.out.println("Order found - Order ID: " + order.getOrderId() + ", Customer ID: " + order.getCustomerId() + ", Requested Customer ID: " + customerId);

            // Verify the order belongs to the authenticated user
            if (!order.getCustomerId().equals(customerId)) {
                System.out.println("Order customer mismatch - Order belongs to: " + order.getCustomerId() + ", but requested by: " + customerId);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                String errorResponse = "{\"error\": \"Order not found\"}";
                byte[] bytes = errorResponse.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, bytes.length);
                exchange.getResponseBody().write(bytes);
                return;
            }

            String response =
                    """
                    {
                      "orderId": "%s",
                      "status": "%s",
                      "totalAmount": %.2f,
                      "version": %d,
                      "shippingAddressId": %s
                    }
                    """.formatted(
                            order.getOrderId(),
                            order.getStatus(),
                            order.totalAmount(),
                            order.getVersion(),
                            order.getShippingAddressId() != null ? "\"" + order.getShippingAddressId() + "\"" : "null"
                    );

            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            exchange.sendResponseHeaders(500, -1);
        }
    }

}
