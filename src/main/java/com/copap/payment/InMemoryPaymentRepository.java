package com.copap.payment;

import java.util.*;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryPaymentRepository implements PaymentRepository {

    private final Map<String, Payment> payments =
            new ConcurrentHashMap<>();

    @Override
    public Optional<Payment> findByOrderId(String orderId) {
        return Optional.ofNullable(payments.get(orderId));
    }

    @Override
    public void save(Payment payment) {
        payments.put(payment.getOrderId(), payment);
    }

    @Override
    public List<Payment> findIncompletePayments() {
        return payments.values().stream()
                .filter(p ->
                        p.getStatus() == PaymentStatus.INITIATED ||
                                p.getStatus() == PaymentStatus.PROCESSING
                )
                .toList();
    }
}
