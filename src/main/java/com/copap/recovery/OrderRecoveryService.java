package com.copap.recovery;

import com.copap.db.TransactionManager;
import com.copap.engine.FailureAwareExecutor;
import com.copap.engine.OrderProcessingTask;
import com.copap.model.Order;
import com.copap.model.OrderStatus;
import com.copap.payment.PaymentService;
import com.copap.repository.OrderRepository;

public class OrderRecoveryService {

    private final OrderRepository repository;
    private final FailureAwareExecutor executor;
    private final PaymentService paymentService;
    private final TransactionManager transactionManager;
    public OrderRecoveryService(OrderRepository repository,
                                FailureAwareExecutor executor, PaymentService paymentService, TransactionManager transactionManager) {
        this.repository = repository;
        this.executor = executor;
        this.paymentService = paymentService;
        this.transactionManager = transactionManager;
    }

    public void recover(Order order) {
        OrderStatus status = order.getStatus();

        if (status == OrderStatus.PAYMENT_PENDING ||
                status == OrderStatus.INVENTORY_RESERVED) {

            System.out.println(
                    "Recovering order " + order.getOrderId() +
                            " from state " + status
            );

            executor.submit(new OrderProcessingTask(order, paymentService,  transactionManager));
        }
    }
}
