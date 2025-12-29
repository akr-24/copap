package com.copap.engine;

import com.copap.model.Order;

import java.util.Random;

public class OrderProcessingTask implements RetryableTask {

    private final Order order;
    private final Random random = new Random();

    public OrderProcessingTask(Order order) {
        this.order = order;
    }

    @Override
    public void run() {
        // simulate flaky dependency
        if (random.nextBoolean()) {
            throw new RuntimeException("Payment gateway timeout");
        }
        System.out.println("Processed order " + order.getOrderId());
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
