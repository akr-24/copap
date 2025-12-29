package com.copap.recovery;

import com.copap.model.Order;
import com.copap.model.OrderStatus;
import com.copap.repository.OrderRepository;
import com.copap.engine.FailureAwareExecutor;
import com.copap.engine.OrderProcessingTask;

public class OrderRecoveryService {

    private final OrderRepository repository;
    private final FailureAwareExecutor executor;

    public OrderRecoveryService(OrderRepository repository,
                                FailureAwareExecutor executor) {
        this.repository = repository;
        this.executor = executor;
    }

    public void recover(Order order) {
        OrderStatus status = order.getStatus();

        if (status == OrderStatus.PAYMENT_PENDING ||
                status == OrderStatus.INVENTORY_RESERVED) {

            System.out.println(
                    "Recovering order " + order.getOrderId() +
                            " from state " + status
            );

            executor.submit(new OrderProcessingTask(order));
        }
    }
}
