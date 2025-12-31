package com.copap.payment;

public interface PaymentGateway {
    PaymentResult charge(String paymentId, String orderId, double amount);
}