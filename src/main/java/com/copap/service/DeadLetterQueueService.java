package com.copap.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DeadLetterQueueService {

    private final JdbcTemplate jdbcTemplate;

    public DeadLetterQueueService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void enqueue(String orderId, String errorMessage) {
        jdbcTemplate.update(
                "INSERT INTO dead_letter_queue (order_id, error_message, created_at, retry_count) VALUES (?, ?, NOW(), 0)",
                orderId, errorMessage
        );
    }
}
