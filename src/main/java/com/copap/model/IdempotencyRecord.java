package com.copap.model;

import java.time.Instant;

public class IdempotencyRecord {

    private final String key;
    private final String orderId;
    private final String requestHash;
    private final Instant createdAt;

    public IdempotencyRecord(String key, String orderId, String requestHash) {
        this.key = key;
        this.orderId = orderId;
        this.requestHash = requestHash;
        this.createdAt = Instant.now();
    }

    public String getKey() { return key; }
    public String getOrderId() { return orderId; }
    public String getRequestHash() { return requestHash; }
}
