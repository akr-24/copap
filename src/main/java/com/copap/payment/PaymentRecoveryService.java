package com.copap.payment;

import com.copap.model.Order;
import com.copap.model.OrderStatus;
import com.copap.repository.OrderRepository;
import com.copap.service.AsyncOrderProcessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentRecoveryService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final AsyncOrderProcessor asyncOrderProcessor;

    public PaymentRecoveryService(PaymentRepository paymentRepository,
                                  OrderRepository orderRepository,
                                  AsyncOrderProcessor asyncOrderProcessor) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.asyncOrderProcessor = asyncOrderProcessor;
    }

    @Transactional
    public void recoverPayments() {
        for (Payment payment : paymentRepository.findIncompletePayments()) {
            Order order = orderRepository.findById(payment.getOrderId()).orElse(null);
            if (order == null) continue;

            if (order.getStatus() == OrderStatus.PAYMENT_PENDING) {
                System.out.println("Recovering payment for order " + order.getOrderId());
                asyncOrderProcessor.processAsync(order.getOrderId());
            }
        }
    }
}
