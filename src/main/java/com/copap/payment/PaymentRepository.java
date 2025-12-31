package com.copap.payment;

import java.util.Optional;

public interface PaymentRepository {

    Optional<Payment> findByOrderId(String orderId);

    void save(Payment payment);
}
