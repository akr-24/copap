package com.copap.payment;

import java.time.Instant;
import java.util.Objects;

public class Payment {

    private final String paymentId;
    private final String orderId;
    private final double amount;

    private PaymentStatus status;
    private final Instant createdAt;

    public Payment(String paymentId, String orderId, double amount) {
        this.paymentId = Objects.requireNonNull(paymentId);
        this.orderId = Objects.requireNonNull(orderId);
        this.amount = amount;
        this.status = PaymentStatus.INITIATED;
        this.createdAt = Instant.now();
    }

    public void markProcessing() {
        this.status = PaymentStatus.PROCESSING;
    }

    public void markSuccess() {
        this.status = PaymentStatus.SUCCESS;
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getOrderId() {
        return orderId;
    }

    public double getAmount() {
        return amount;
    }
}
