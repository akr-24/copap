package com.copap.engine;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DeadLetterQueue {

    private final Queue<RetryableTask> failedTasks =
            new ConcurrentLinkedQueue<>();

    public void add(RetryableTask task) {
        failedTasks.add(task);
    }

    public int size() {
        return failedTasks.size();
    }
}
