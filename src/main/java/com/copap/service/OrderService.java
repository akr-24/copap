package com.copap.service;

import com.copap.model.*;
import com.copap.repository.*;
import com.copap.analytics.*;
import java.util.Objects;
import java.util.List;

public class OrderService {

    private final OrderRepository orderRepository;
    private final IdempotencyRepository idempotencyRepository;

    public OrderService(OrderRepository repository, IdempotencyRepository idempotencyRepository) {
        this.orderRepository = repository;
        this.idempotencyRepository = idempotencyRepository;
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

        orderRepository.save(order);
    }

    public Order createOrder(String idempotencyKey, Customer customer, List<Product> products) {
        String requestHash = Objects.hash(customer.getCustomerId(), products) + "";

        return idempotencyRepository.find(idempotencyKey)
                .map(record -> orderRepository.findById(record.getOrderId()).orElseThrow()).orElseGet(() -> {

                    String orderId = "O-" + System.nanoTime();
                    Order order = new Order(orderId, customer, products);

                    orderRepository.save(order);

                    idempotencyRepository.save(
                            new IdempotencyRecord(
                                    idempotencyKey,
                                    orderId,
                                    requestHash
                            )
                    );
//                    analyticsService.publish(
//                            new OrderEvent(order.getOrderId(), order.totalAmount())
//                    );
                    return order;
                });
    }

    public void advanceOrder(String orderId, OrderStatus nextStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        order.updateStatus(nextStatus);
    }

    public void advanceOrderWithVersion(
            String orderId,
            OrderStatus nextStatus,
            long expectedVersion
    ) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        order.updateStatus(nextStatus);

        ((CachedOrderRepository) orderRepository).updateWithVersion(order, expectedVersion);
    }
}