package com.copap.service;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncOrderProcessor {

    private final OrderService orderService;
    private final DeadLetterQueueService dlqService;

    public AsyncOrderProcessor(OrderService orderService, DeadLetterQueueService dlqService) {
        this.orderService = orderService;
        this.dlqService = dlqService;
    }

    @Async("orderExecutor")
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void processAsync(String orderId) {
        orderService.processOrderPayment(orderId);
    }

    @Recover
    public void onMaxRetriesReached(RuntimeException e, String orderId) {
        System.err.println("Order " + orderId + " exhausted all retries: " + e.getMessage());
        dlqService.enqueue(orderId, e.getMessage());
    }
}
