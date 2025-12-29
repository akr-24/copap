package com.copap.repository;

import com.copap.model.Order;

import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class InMemoryOrderRepository implements OrderRepository {

    //    private final Map<String, Order> store = new HashMap<>();
   // concurrentHashMap guarantees synchronization, i.e. it is thread-safe by design
    private final ConcurrentMap<String, Order> store = new ConcurrentHashMap<>();

    @Override
    public void save(Order order) {
        Order existing = store.putIfAbsent(order.getOrderId(), order);
        if (existing != null) {
            throw new IllegalStateException("Order already exists");
        }
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