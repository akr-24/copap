package com.copap.model;

import java.util.concurrent.atomic.AtomicLong;

public class VersionedOrder {

    private final Order order;
    private final AtomicLong version = new AtomicLong(0);

    public VersionedOrder(Order order) {
        this.order = order;
    }

    public Order getOrder() {
        return order;
    }

    public long getVersion() {
        return version.get();
    }

    public long incrementVersion() {
        return version.incrementAndGet();
    }
}
