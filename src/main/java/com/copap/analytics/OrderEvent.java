package com.copap.analytics;

import java.time.Instant;

public class OrderEvent {

    private final String orderId;
    private final double amount;
    private final Instant timestamp;

    public OrderEvent(String orderId, double amount) {
        this.orderId = orderId;
        this.amount = amount;
        this.timestamp = Instant.now();
    }

    public double getAmount() {
        return amount;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
