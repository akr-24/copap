package com.copap.model;

public enum OrderStatus {
    NEW,
    VALIDATED,
    INVENTORY_RESERVED,
    PAYMENT_PENDING,
    PAID,
    SHIPPED,
    FAILED
}