package com.copap.repository;

import com.copap.model.Order;

import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, Order> store = new HashMap<>();

    @Override
    public void save(Order order) {
        store.put(order.getOrderId(), order);
    }

    @Override
    public Optional < Order > findById ( String orderId) {
        return Optional.ofNullable(store.get(orderId));
    }

    @Override
    public boolean exists(String orderId) {
        return store.containsKey(orderId);
    }
}