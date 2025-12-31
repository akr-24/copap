package com.copap.engine;

import com.copap.model.Order;
import com.copap.payment.PaymentService;
import com.copap.payment.PaymentResult;
import com.copap.model.OrderStatus;
import java.util.Random;

public class OrderProcessingTask implements RetryableTask {

    private final Order order;
    private final PaymentService paymentService;

    public OrderProcessingTask(Order order, PaymentService paymentService) {
        this.paymentService = paymentService;
        this.order = order;
    }

    @Override
    public void run() {
        if(order.getStatus() == OrderStatus.PAYMENT_PENDING) {
            PaymentResult result = paymentService.processPayment(order.getOrderId(), order.totalAmount());
            System.out.println("order processing task babababbab");
            switch (result) {
                case SUCCESS -> {
                    order.updateStatus(OrderStatus.PAID);
                    System.out.println(
                            "Payment successful for order " +
                                    order.getOrderId()
                    );
                }

                case FAILED -> {
                    order.updateStatus(OrderStatus.FAILED);
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
        }
    }

    @Override
    public int maxRetries() {
        return 3;
    }

    @Override
    public void onFailure(Exception e) {
        System.out.println(
                "Order " + order.getOrderId() + " moved to DLQ: " + e.getMessage()
        );
    }
}
