package com.copap.engine;

public interface RetryableTask extends Runnable {

    int maxRetries();

    void onFailure(Exception e);

    default long backoffMillis(int attempt) {
        return (long) Math.pow(2, attempt) * 100;
    }
}
