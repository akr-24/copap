package com.copap.repository;

import com.copap.model.Order;
import com.copap.model.VersionedOrder;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Primary
public class CachedOrderRepository implements OrderRepository {

    private final JdbcOrderRepository delegate;
    private final ConcurrentMap<String, VersionedOrder> cache = new ConcurrentHashMap<>();

    public CachedOrderRepository(JdbcOrderRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public void save(Order order) {
        delegate.save(order);

        cache.compute(order.getOrderId(), (id, existing) -> {
            if (existing == null) return new VersionedOrder(order);
            existing.incrementVersion();
            return existing;
        });
    }

    @Override
    public void updateWithVersion(Order order, long expectedVersion) {
        // Fix for Bug #7: delegate must be called first so the DB optimistic lock
        // check runs. Only update the cache after the DB write succeeds.
        delegate.updateWithVersion(order, expectedVersion);

        cache.compute(order.getOrderId(), (id, existing) -> {
            if (existing != null) {
                existing.incrementVersion();
            }
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
        fromRepo.ifPresent(order -> cache.put(orderId, new VersionedOrder(order)));
        return fromRepo;
    }

    @Override
    public boolean exists(String orderId) {
        return cache.containsKey(orderId) || delegate.exists(orderId);
    }

    @Override
    public List<Order> findByCustomerId(String customerId) {
        return delegate.findByCustomerId(customerId);
    }

    public long getCachedVersion(String orderId) {
        VersionedOrder vo = cache.get(orderId);
        if (vo == null) throw new IllegalStateException("Order not found in cache: " + orderId);
        return vo.getVersion();
    }
}
