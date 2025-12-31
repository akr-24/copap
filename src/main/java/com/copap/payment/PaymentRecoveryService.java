package com.copap.payment;

import com.copap.engine.FailureAwareExecutor;
import com.copap.engine.OrderProcessingTask;
import com.copap.model.Order;
import com.copap.model.OrderStatus;
import com.copap.repository.OrderRepository;

public class PaymentRecoveryService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final FailureAwareExecutor executor;
    private final PaymentService paymentService;

    public PaymentRecoveryService(PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            FailureAwareExecutor executor,
            PaymentService paymentService) {

        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.executor = executor;
        this.paymentService = paymentService;
    }

    public void recoverPayments() {

        for (Payment payment : paymentRepository.findIncompletePayments()) {

            Order order = orderRepository
                    .findById(payment.getOrderId())
                    .orElse(null);

            if (order == null) continue;

            if (order.getStatus() == OrderStatus.PAYMENT_PENDING) {

                System.out.println(
                        "Recovering payment for order " +
                                order.getOrderId()
                );

                executor.submit(
                        new OrderProcessingTask(order, paymentService)
                );
            }
        }
    }
}
