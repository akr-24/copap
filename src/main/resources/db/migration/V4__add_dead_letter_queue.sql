-- V4: DB-backed Dead Letter Queue
-- Replaces in-memory DeadLetterQueue (lost on restart).
-- Stores orders that exhausted all payment retry attempts.

CREATE TABLE IF NOT EXISTS dead_letter_queue (
    id            SERIAL       PRIMARY KEY,
    order_id      VARCHAR(36)  NOT NULL,
    error_message TEXT,
    retry_count   INT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dlq_order ON dead_letter_queue(order_id);
