package com.copap.payment;

import java.util.UUID;

public class PaymentService {

    private final PaymentGateway gateway;
    private final PaymentRepository repository;

    public PaymentService(PaymentGateway gateway,
                          PaymentRepository repository) {
        this.gateway = gateway;
        this.repository = repository;
    }

    public PaymentResult processPayment(String orderId,
                                        double amount) {

        // Idempotency check
        Payment payment = repository.findByOrderId(orderId)
                .orElseGet(() -> {
                    Payment p = new Payment(
                            UUID.randomUUID().toString(),
                            orderId,
                            amount
                    );
                    repository.save(p);
                    return p;
                });

        // If already completed, return result
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return PaymentResult.SUCCESS;
        }
        if (payment.getStatus() == PaymentStatus.FAILED) {
            return PaymentResult.FAILED;
        }

        // Attempt payment
        payment.markProcessing();

        PaymentResult result =
                gateway.charge(
                        payment.getOrderId(),
                        payment.getOrderId(),
                        payment.getAmount()
                );

        // Update state based on result
        switch (result) {
            case SUCCESS -> payment.markSuccess();
            case FAILED -> payment.markFailed();
            case RETRYABLE_FAILURE -> {
                // do nothing, retry later
            }
        }

        return result;
    }
}
