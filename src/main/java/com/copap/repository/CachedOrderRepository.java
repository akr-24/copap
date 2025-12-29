package com.copap.repository;

import com.copap.model.Order;
import com.copap.model.VersionedOrder;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CachedOrderRepository implements OrderRepository {

    private final OrderRepository delegate;
    private final ConcurrentMap<String, VersionedOrder> cache =
            new ConcurrentHashMap<>();

    public CachedOrderRepository(OrderRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public void save(Order order) {
        delegate.save(order);

        cache.compute(order.getOrderId(), (id, existing) -> {
            if (existing == null) {
                return new VersionedOrder(order);
            }
            existing.incrementVersion();
            return existing;
        });
    }

    @Override
    public Optional<Order> findById(String orderId) {
        VersionedOrder cached = cache.get(orderId);
        if (cached != null) {
            return Optional.of(cached.getOrder());
        }

        Optional<Order> fromRepo = delegate.findById(orderId);
        fromRepo.ifPresent(order ->
                cache.put(orderId, new VersionedOrder(order)));

        return fromRepo;
    }

    @Override
    public boolean exists(String orderId) {
        return cache.containsKey(orderId) || delegate.exists(orderId);
    }
}
