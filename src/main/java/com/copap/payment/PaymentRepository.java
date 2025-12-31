package com.copap.payment;

import java.util.Optional;
import java.util.List;

public interface PaymentRepository {

    Optional<Payment> findByOrderId(String orderId);

    void save(Payment payment);

    List<Payment> findIncompletePayments();
}
