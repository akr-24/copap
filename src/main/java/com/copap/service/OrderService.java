package com.copap.service;

import com.copap.db.TransactionManager;
import com.copap.engine.FailureAwareExecutor;
import com.copap.engine.OrderProcessingTask;
import com.copap.model.IdempotencyRecord;
import com.copap.model.Order;
import com.copap.model.OrderStatus;
import com.copap.payment.PaymentService;
import com.copap.repository.IdempotencyRepository;
import com.copap.repository.OrderRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class OrderService {

    private final OrderRepository orderRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final PaymentService paymentService;
    private final TransactionManager transactionManager;
    private final FailureAwareExecutor executor;
    public OrderService(OrderRepository orderRepository,
                        IdempotencyRepository idempotencyRepository,
                        PaymentService paymentService,
                        TransactionManager transactionManager,
                        FailureAwareExecutor executor) {

        this.orderRepository = orderRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.paymentService = paymentService;
        this.transactionManager = transactionManager;
        this.executor = executor;
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

    public Order createOrder(String idempotencyKey,
                             String requestHash,
                             String customerId,
                             List<String> productIds,
                             double totalAmount) {

        // 1. Generate a candidate orderId
        String candidateOrderId = UUID.randomUUID().toString();

        // 2. Ask idempotency layer to decide
        IdempotencyRecord record =
                idempotencyRepository.saveOrGet(
                        idempotencyKey,
                        requestHash,
                        candidateOrderId
                );

        // 3. use the orderId from the record
        String finalOrderId = record.getOrderId();

        // 4. If this was a duplicate request → order already exists
        Optional<Order> existing =
                orderRepository.findById(finalOrderId);

        if (existing.isPresent()) {
            return existing.get();   // idempotent return
        }

        // 5. Otherwise, create the order ONCE
        Order order = new Order(finalOrderId, customerId, productIds, totalAmount);

        orderRepository.save(order);
        return order;
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

        orderRepository.updateWithVersion(order, expectedVersion);
    }

    public void startPaymentProcessing(String orderId) {
        executor.submit(
                new OrderProcessingTask(
                        orderId,
                        this,
                        orderRepository,
                        paymentService,
                        transactionManager
                )
        );
    }

    public Optional<Order> getOrder(String orderId) {
        return orderRepository.findById(orderId);
    }
}