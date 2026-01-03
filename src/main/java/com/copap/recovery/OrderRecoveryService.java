package com.copap.recovery;

import com.copap.db.TransactionManager;
import com.copap.engine.FailureAwareExecutor;
import com.copap.engine.OrderProcessingTask;
import com.copap.model.Order;
import com.copap.model.OrderStatus;
import com.copap.payment.PaymentService;
import com.copap.repository.OrderRepository;
import com.copap.service.OrderService;

public class OrderRecoveryService {

    private final OrderRepository repository;
    private final FailureAwareExecutor executor;
    private final PaymentService paymentService;
    private final TransactionManager transactionManager;
    private final OrderService orderService;
    public OrderRecoveryService(OrderRepository repository,
                                FailureAwareExecutor executor,
                                PaymentService paymentService,
                                TransactionManager transactionManager,
                                OrderService orderService) {
        this.repository = repository;
        this.executor = executor;
        this.paymentService = paymentService;
        this.transactionManager = transactionManager;
        this.orderService = orderService;
    }

    public void recover(Order order) {
        OrderStatus status = order.getStatus();

        if (status == OrderStatus.PAYMENT_PENDING ||
                status == OrderStatus.INVENTORY_RESERVED) {

            System.out.println(
                    "Recovering order " + order.getOrderId() +
                            " from state " + status
            );

            executor.submit(new OrderProcessingTask(order.getOrderId(), orderService, repository, paymentService,  transactionManager));
        }
    }
}
