package com.copap.analytics;

import com.copap.events.OrderPlacedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

    private final SlidingWindowAnalytics analytics;

    public AnalyticsService(@Value("${app.analytics.window-ms:300000}") long windowMs) {
        this.analytics = new SlidingWindowAnalytics(windowMs);
    }

    @EventListener
    public void onOrderPlaced(OrderPlacedEvent event) {
        analytics.record(new OrderEvent(event.orderId(), event.amount()));
    }

    public long getOrderCount() {
        return analytics.getOrderCount();
    }

    public double getRevenue() {
        return analytics.getRevenue();
    }
}
