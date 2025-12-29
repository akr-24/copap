package com.copap.analytics;

import com.copap.engine.FailureAwareExecutor;
import com.copap.engine.RetryableTask;

public class AnalyticsService {

    private final SlidingWindowAnalytics analytics;
    private final FailureAwareExecutor executor;

    public AnalyticsService(SlidingWindowAnalytics analytics,
                            FailureAwareExecutor executor) {
        this.analytics = analytics;
        this.executor = executor;
    }

    public void publish(OrderEvent event) {
        executor.submit(new RetryableTask() {
            @Override
            public void run() {
                analytics.record(event);
            }

            @Override
            public int maxRetries() {
                return 2;
            }

            @Override
            public void onFailure(Exception e) {
                System.out.println("Analytics event dropped");
            }
        });
    }
}
