package com.copap.model;

import java.util.Objects;
import java.util.List;
import java.time.Instant;
import java.util.Collections;


public class Order {
    private final String orderId;
    private final Customer customer;
    private final List<Product> products;
    private final Instant createdAt;

    private OrderStatus orderStatus;

    public Order(String orderId, Customer customer, List<Product> products) {
        this.orderId = Objects.requireNonNull(orderId);
        this.customer = Objects.requireNonNull(customer);
        this.products = Collections.unmodifiableList(products);
        this.createdAt = Instant.now();
        this.orderStatus = OrderStatus.NEW;
    }

    public String getOrderId() { return orderId; }
    public Customer getCustomer() { return customer; }
    public List<Product> getProducts() { return products; }
    public Instant getCreatedAt() { return createdAt; }
    public OrderStatus getStatus() { return orderStatus; }

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
        return products.stream()
                .mapToDouble(Product::getPrice)
                .sum();
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