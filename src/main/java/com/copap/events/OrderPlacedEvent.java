package com.copap.events;

public record OrderPlacedEvent(String orderId, double amount) {}
