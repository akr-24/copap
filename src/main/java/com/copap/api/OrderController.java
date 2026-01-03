package com.copap.api;

import com.copap.db.TransactionManager;
import com.copap.engine.FailureAwareExecutor;
import com.copap.engine.OrderProcessingTask;
import com.copap.model.Order;
import com.copap.model.OrderStatus;
import com.copap.payment.PaymentService;
import com.copap.product.ProductRepository;
import com.copap.service.OrderService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class OrderController implements HttpHandler {

    private final OrderService orderService;
    private final ProductRepository productRepository;
    private final PaymentService paymentService;
    private final TransactionManager transactionManager;
    private final FailureAwareExecutor executor;

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
                            customerId,
                            productIds,
                            totalAmount
                    );
            //mark payment pending
//            orderService.advanceOrderWithVersion(order.getOrderId(), OrderStatus.VALIDATED, order.getVersion());
//            orderService.advanceOrderWithVersion(order.getOrderId(), OrderStatus.INVENTORY_RESERVED, order.getVersion() +1);
//            orderService.advanceOrderWithVersion(order.getOrderId(), OrderStatus.PAYMENT_PENDING, order.getVersion());
            // trigger the payment processing
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
