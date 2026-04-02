package com.copap.payment;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentGateway gateway;
    private final PaymentRepository repository;

    public PaymentService(PaymentGateway gateway, PaymentRepository repository) {
        this.gateway = gateway;
        this.repository = repository;
    }

    public PaymentResult processPayment(String orderId, double amount) {
        Payment payment = repository.findByOrderId(orderId).orElseGet(() -> {
            Payment p = new Payment(UUID.randomUUID().toString(), orderId, amount);
            repository.save(p);
            return p;
        });

        if (payment.getStatus() == PaymentStatus.SUCCESS) return PaymentResult.SUCCESS;
        if (payment.getStatus() == PaymentStatus.FAILED)  return PaymentResult.FAILED;

        payment.markProcessing();

        PaymentResult result = gateway.charge(
                payment.getPaymentId(),
                payment.getOrderId(),
                payment.getAmount()
        );

        // Fix for Bug #8: persist final payment status to DB after gateway response.
        switch (result) {
            case SUCCESS -> { payment.markSuccess(); repository.update(payment); }
            case FAILED  -> { payment.markFailed();  repository.update(payment); }
            case RETRYABLE_FAILURE -> { /* status stays PROCESSING; will be retried */ }
        }

        return result;
    }
}
