package com.copap.engine;

import com.copap.db.TransactionManager;
import com.copap.model.Order;
import com.copap.model.OrderStatus;
import com.copap.payment.PaymentResult;
import com.copap.payment.PaymentService;
import com.copap.repository.OrderRepository;
import com.copap.service.OrderService;

public class OrderProcessingTask implements RetryableTask {

    private final String orderId;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final TransactionManager transactionManager;

    public OrderProcessingTask(String orderId, OrderService orderService, OrderRepository orderRepository, PaymentService paymentService, TransactionManager transactionManager) {
        this.paymentService = paymentService;
        this.orderId = orderId;
        this.transactionManager = transactionManager;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    public void run() {
        transactionManager.executeInTransaction(() -> {

            // Re-read fresh order from DB
            Order order = orderRepository
                    .findById(orderId)
                    .orElseThrow();

            if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
                return null;
            }

            PaymentResult result =
                    paymentService.processPayment(
                            order.getOrderId(),
                            order.totalAmount()
                    );

            switch (result) {
                case SUCCESS -> {
                    // First transition to PAID
                    orderService.advanceOrderWithVersion(
                            order.getOrderId(),
                            OrderStatus.PAID,
                            order.getVersion()
                    );
                    System.out.println(
                            "Payment successful for order " +
                                    order.getOrderId()
                    );
                    
                    // Then transition to SHIPPED (simulating immediate shipping)
                    Order paidOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
                    orderService.advanceOrderWithVersion(
                            paidOrder.getOrderId(),
                            OrderStatus.SHIPPED,
                            paidOrder.getVersion()
                    );
                    System.out.println(
                            "Order shipped: " + order.getOrderId()
                    );
                }

                case FAILED -> {
                    orderService.advanceOrderWithVersion(
                            order.getOrderId(),
                            OrderStatus.FAILED,
                            order.getVersion()
                    );
                    System.out.println(
                            "Payment failed permanently for order " +
                                    order.getOrderId()
                    );
                }

                case RETRYABLE_FAILURE -> {
                    System.out.println(
                            "Payment retry needed for order " +
                                    order.getOrderId()
                    );
                    throw new RuntimeException("Retry payment");
                }
            }

            return null;
        });
    }

    @Override
    public int maxRetries() {
        return 3;
    }

    @Override
    public void onFailure(Exception e) {
        System.out.println(
                "Order " + orderId + " moved to DLQ: " + e.getMessage()
        );
    }
}
