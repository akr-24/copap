package com.copap.service;

import com.copap.repository.OrderRepository;
import com.copap.model.Order;
import com.copap.model.OrderStatus;

public class OrderService {

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    public void createOrder(Order order) {
        /**
         * this will not handle concurrency as Multiple threads
         * can create the order because the existence check and save are not atomic.
         *

        if (repository.exists(order.getOrderId())) {
            throw new IllegalStateException("Order already exists");
        }
         */

        repository.save(order);
    }

    public void advanceOrder(String orderId, OrderStatus nextStatus) {
        Order order = repository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        order.updateStatus(nextStatus);
    }
}