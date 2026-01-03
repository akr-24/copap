package com.copap.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;


public class Order {
    private final String orderId;
    private final String customerId;
    private final List<String> productIds;
    private final Instant createdAt;

    private OrderStatus orderStatus;
    private final AtomicLong version;
    private double totalAmount;

    public Order(String orderId, String customerId, List<String> productIds, double totalAmount) {
        this.orderId = Objects.requireNonNull(orderId);
        this.customerId = Objects.requireNonNull(customerId);
        this.productIds = List.copyOf(productIds);
        this.createdAt = Instant.now();
        this.orderStatus = OrderStatus.NEW;
        this.version = new AtomicLong(0);
        this.totalAmount = totalAmount; // will be set externally
    }

    public String getOrderId() { return orderId; }
    public String getCustomerId() {
        return customerId;
    }

    public List<String> getProductIds() {
        return productIds;
    }
    public Instant getCreatedAt() { return createdAt; }
    public OrderStatus getStatus() { return orderStatus; }
    public long getVersion() {
        return version.get();
    }
    // since state-transitions must be atomic, hence it is being coverted into synchronized
    public synchronized void updateStatus(OrderStatus newStatus) {
        if (!OrderStateMachine.canTransition(this.orderStatus, newStatus)) {
            throw new IllegalStateException(
                    "Illegal transition: " + this.orderStatus + " → " + newStatus
            );
        }
        this.orderStatus = newStatus;
    }


    public double totalAmount() {
        return totalAmount;
    }

    public static Order fromDb(
            String orderId,
            OrderStatus status,
            String customerId,
            List<String> productIds,
            double totalAmount,
            long version
    ) {
        Order order = new Order(orderId, customerId, productIds, totalAmount);
        order.orderStatus = status;
        order.version.set(version);
        order.totalAmount = totalAmount;
        return order;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order)) return false;
        Order order = (Order) o;
        return orderId.equals(order.orderId);
    }

    @Override
    public int hashCode() {
        return orderId.hashCode();
    }

}