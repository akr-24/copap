-- V2: order_items join table
-- Fixes Bug #4: productIds was always returned as an empty list because
-- there was no table to persist/retrieve the order-to-product relationship.

CREATE TABLE IF NOT EXISTS order_items (
    order_id   VARCHAR(36) NOT NULL REFERENCES orders(order_id),
    product_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (order_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_order_items_order ON order_items(order_id);
