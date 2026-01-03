package com.copap.api;

import com.copap.engine.OrderProcessingTask;
import com.copap.model.Order;
import com.copap.product.ProductRepository;
import com.copap.service.OrderService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class OrderController implements HttpHandler {

    private final OrderService orderService;
    private final ProductRepository productRepository;

    public OrderController(OrderService orderService,
                           ProductRepository productRepository) {
        this.orderService = orderService;
        this.productRepository = productRepository;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        System.out.println("OrderController hit");

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String idempotencyKey =
                exchange.getRequestHeaders().getFirst("Idempotency-Key");

        if (idempotencyKey == null) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }

        try {
            // TEMP hardcoded input (JSON parsing comes next)
            String customerId = "C1";
            var productIds = java.util.List.of("P1");

            var products = productRepository.findByIds(productIds);

            double totalAmount =
                    products.stream().mapToDouble(p -> p.getPrice()).sum();

            String requestHash =
                    customerId + "|" + productIds + "|" + totalAmount;

            String orderId = java.util.UUID.randomUUID().toString();

            Order order =
                    orderService.createOrder(
                            idempotencyKey,
                            requestHash,
                            orderId,
                            customerId,
                            productIds,
                            totalAmount
                    );

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

            byte[] bytes = response.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            exchange.getResponseHeaders().add(
                    "Content-Type", "application/json"
            );

            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);

        } catch (Exception e) {
            e.printStackTrace();
            exchange.sendResponseHeaders(500, -1);
        } finally {
            exchange.close();
        }
    }

}
