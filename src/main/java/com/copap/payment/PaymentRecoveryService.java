package com.copap.payment;

import com.copap.db.TransactionManager;
import com.copap.engine.FailureAwareExecutor;
import com.copap.engine.OrderProcessingTask;
import com.copap.model.Order;
import com.copap.model.OrderStatus;
import com.copap.repository.OrderRepository;
import com.copap.service.OrderService;

public class PaymentRecoveryService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final FailureAwareExecutor executor;
    private final PaymentService paymentService;
    private final TransactionManager transactionManager;
    private final OrderService orderService;

    public PaymentRecoveryService(PaymentRepository paymentRepository,
                                  OrderRepository orderRepository,
                                  FailureAwareExecutor executor,
                                  PaymentService paymentService,
                                  TransactionManager transactionManager,
                                  OrderService orderService) {

        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.executor = executor;
        this.paymentService = paymentService;
        this.transactionManager = transactionManager;
        this.orderService  = orderService;
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
                        new OrderProcessingTask(order.getOrderId(), orderService, orderRepository, paymentService, transactionManager)
                );
            }
        }
    }
}
