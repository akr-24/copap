package com.copap.analytics;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

public class SlidingWindowAnalytics {

    // bucketStartMillis → counters
    private final Map<Long, LongAdder> orderCount = new ConcurrentHashMap<>();
    private final Map<Long, DoubleAdder> revenue = new ConcurrentHashMap<>();

    private final long windowSizeMillis;

    public SlidingWindowAnalytics(long windowSizeMillis) {
        this.windowSizeMillis = windowSizeMillis;
    }

    public void record(OrderEvent event) {
        long bucket = event.getTimestamp().toEpochMilli() / 1000 * 1000;

        orderCount.computeIfAbsent(bucket, b -> new LongAdder()).increment();
        revenue.computeIfAbsent(bucket, b -> new DoubleAdder()).add(event.getAmount());

        cleanupOldBuckets();
    }

    private void cleanupOldBuckets() {
        long cutoff = Instant.now().toEpochMilli() - windowSizeMillis;
        orderCount.keySet().removeIf(ts -> ts < cutoff);
        revenue.keySet().removeIf(ts -> ts < cutoff);
    }

    public long getOrderCount() {
        return orderCount.values().stream()
                .mapToLong(LongAdder::sum)
                .sum();
    }

    public double getRevenue() {
        return revenue.values().stream()
                .mapToDouble(DoubleAdder::sum)
                .sum();
    }
}
