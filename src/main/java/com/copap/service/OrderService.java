package com.copap.service;

import com.copap.api.exception.OrderNotFoundException;
import com.copap.events.OrderPlacedEvent;
import com.copap.model.IdempotencyRecord;
import com.copap.model.Order;
import com.copap.model.OrderStatus;
import com.copap.payment.PaymentResult;
import com.copap.payment.PaymentService;
import com.copap.repository.IdempotencyRepository;
import com.copap.repository.OrderRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final PaymentService paymentService;
    private final AsyncOrderProcessor asyncOrderProcessor;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository,
                        IdempotencyRepository idempotencyRepository,
                        PaymentService paymentService,
                        @Lazy AsyncOrderProcessor asyncOrderProcessor,
                        ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.paymentService = paymentService;
        this.asyncOrderProcessor = asyncOrderProcessor;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Order createOrder(String idempotencyKey,
                             String requestHash,
                             String customerId,
                             List<String> productIds,
                             double totalAmount) {
        return createOrder(idempotencyKey, requestHash, customerId, productIds, totalAmount, null);
    }

    @Transactional
    public Order createOrder(String idempotencyKey,
                             String requestHash,
                             String customerId,
                             List<String> productIds,
                             double totalAmount,
                             String shippingAddressId) {

        String candidateOrderId = UUID.randomUUID().toString();

        IdempotencyRecord record = idempotencyRepository.saveOrGet(
                idempotencyKey, requestHash, candidateOrderId
        );

        String finalOrderId = record.getOrderId();

        Optional<Order> existing = orderRepository.findById(finalOrderId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Order order = new Order(finalOrderId, customerId, productIds, totalAmount, shippingAddressId);
        orderRepository.save(order);

        eventPublisher.publishEvent(new OrderPlacedEvent(finalOrderId, totalAmount));

        return order;
    }

    @Transactional
    public void advanceOrder(String orderId, OrderStatus nextStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.updateStatus(nextStatus);
        orderRepository.save(order);
    }

    // Not @Transactional — intended to be called from within an existing @Transactional boundary.
    public void advanceOrderWithVersion(String orderId, OrderStatus nextStatus, long expectedVersion) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.updateStatus(nextStatus);
        orderRepository.updateWithVersion(order, expectedVersion);
    }

    // The actual payment processing logic runs inside a @Transactional boundary.
    // Called from AsyncOrderProcessor which wraps it with @Async + @Retryable.
    @Transactional
    public void processOrderPayment(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            return;
        }

        PaymentResult result = paymentService.processPayment(order.getOrderId(), order.totalAmount());

        switch (result) {
            case SUCCESS -> {
                advanceOrderWithVersion(order.getOrderId(), OrderStatus.PAID, order.getVersion());
                Order paidOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
                advanceOrderWithVersion(paidOrder.getOrderId(), OrderStatus.SHIPPED, paidOrder.getVersion());
            }
            case FAILED -> advanceOrderWithVersion(order.getOrderId(), OrderStatus.FAILED, order.getVersion());
            case RETRYABLE_FAILURE -> throw new RuntimeException("Retryable payment failure for order " + orderId);
        }
    }

    public void startPaymentProcessing(String orderId) {
        asyncOrderProcessor.processAsync(orderId);
    }

    public Optional<Order> getOrder(String orderId) {
        return orderRepository.findById(orderId);
    }

    public List<Order> getOrdersByCustomer(String customerId) {
        return orderRepository.findByCustomerId(customerId);
    }
}
