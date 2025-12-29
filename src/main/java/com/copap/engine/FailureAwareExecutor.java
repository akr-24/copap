package com.copap.engine;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FailureAwareExecutor {

    private final ExecutorService executor;
    private final DeadLetterQueue dlq;

    public FailureAwareExecutor(int threads, DeadLetterQueue dlq) {
        this.executor = Executors.newFixedThreadPool(threads);
        this.dlq = dlq;
    }

    public void submit(RetryableTask task) {
        execute(task, 0);
    }

    private void execute(RetryableTask task, int attempt) {
        executor.submit(() -> {
            try {
                task.run();
            } catch (Exception e) {
                if (attempt < task.maxRetries()) {
                    try {
                        Thread.sleep(task.backoffMillis(attempt));
                    } catch (InterruptedException ignored) {}

                    execute(task, attempt + 1);
                } else {
                    task.onFailure(e);
                    dlq.add(task);
                }
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}
