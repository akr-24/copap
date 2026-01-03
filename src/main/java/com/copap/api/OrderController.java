package com.copap.api;

import com.copap.api.dto.CreateOrderRequest;
import com.copap.db.TransactionManager;
import com.copap.engine.FailureAwareExecutor;
import com.copap.model.Order;
import com.copap.payment.PaymentService;
import com.copap.product.ProductRepository;
import com.copap.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class OrderController implements HttpHandler {

    private final OrderService orderService;
    private final ProductRepository productRepository;
    private final PaymentService paymentService;
    private final TransactionManager transactionManager;
    private final FailureAwareExecutor executor;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public OrderController(OrderService orderService,
                           ProductRepository productRepository,
                           PaymentService paymentService,
                           TransactionManager transactionManager,
                           FailureAwareExecutor executor) {

        this.orderService = orderService;
        this.productRepository = productRepository;
        this.paymentService = paymentService;
        this.transactionManager = transactionManager;
        this.executor = executor;
    }

    private void handlePost(HttpExchange exchange) throws IOException {

    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        System.out.println("OrderController hit");

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            // -------------------------
            // POST /orders  (Create)
            // -------------------------
            if (method.equalsIgnoreCase("POST") && path.equals("/orders")) {
                handleCreateOrder(exchange);
                return;
            }

            // -------------------------
            // GET /orders/{orderId}
            // -------------------------
            if (method.equalsIgnoreCase("GET") && path.startsWith("/orders/")) {
                handleGetOrder(exchange);
                return;
            }

            // -------------------------
            // Unsupported route
            // -------------------------
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

        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        System.out.println("RAW BODY => " + body);

        CreateOrderRequest request =
                objectMapper.readValue(body, CreateOrderRequest.class);

        String customerId = request.customerId;
        List<String> productIds = request.productIds;

        var products = productRepository.findByIds(productIds);

        double totalAmount =
                products.stream().mapToDouble(p -> p.getPrice()).sum();

        String requestHash =
                customerId + "|" + productIds + "|" + totalAmount;

        Order order =
                orderService.createOrder(
                        idempotencyKey,
                        requestHash,
                        customerId,
                        productIds,
                        totalAmount
                );

        // Trigger async payment processing
        orderService.startPaymentProcessing(order.getOrderId());

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

    private void handleGetOrder(HttpExchange exchange) throws IOException {

        String path = exchange.getRequestURI().getPath();
        String orderId = path.substring("/orders/".length());

        Order order = orderService
                .getOrder(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        String response =
                """
                {
                  "orderId": "%s",
                  "status": "%s",
                  "totalAmount": %.2f,
                  "version": %d
                }
                """.formatted(
                        order.getOrderId(),
                        order.getStatus(),
                        order.totalAmount(),
                        order.getVersion()
                );

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

}
