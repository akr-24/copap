-- COPAP initial schema
-- V1: All base tables

CREATE TABLE IF NOT EXISTS users (
    user_id     VARCHAR(36)  PRIMARY KEY,
    username    VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email       VARCHAR(255),
    role        VARCHAR(10)  NOT NULL DEFAULT 'USER'
);

CREATE TABLE IF NOT EXISTS auth_tokens (
    token      VARCHAR(36)  PRIMARY KEY,
    user_id    VARCHAR(36)  NOT NULL REFERENCES users(user_id),
    expires_at TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS products (
    product_id VARCHAR(36)   PRIMARY KEY,
    name       VARCHAR(255)  NOT NULL,
    price      DOUBLE PRECISION NOT NULL,
    active     BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS addresses (
    address_id  VARCHAR(36)  PRIMARY KEY,
    user_id     VARCHAR(36)  NOT NULL REFERENCES users(user_id),
    label       VARCHAR(100),
    full_name   VARCHAR(255) NOT NULL,
    phone       VARCHAR(30),
    street      VARCHAR(255) NOT NULL,
    city        VARCHAR(100) NOT NULL,
    state       VARCHAR(100),
    postal_code VARCHAR(20)  NOT NULL,
    country     VARCHAR(100) NOT NULL DEFAULT 'India',
    is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS orders (
    order_id           VARCHAR(36)      PRIMARY KEY,
    status             VARCHAR(30)      NOT NULL,
    customer_id        VARCHAR(36)      NOT NULL,
    total_amount       DOUBLE PRECISION NOT NULL,
    version            BIGINT           NOT NULL DEFAULT 0,
    created_at         TIMESTAMP        NOT NULL DEFAULT NOW(),
    shipping_address_id VARCHAR(36)
);

CREATE TABLE IF NOT EXISTS idempotency_records (
    idempotency_key VARCHAR(255) PRIMARY KEY,
    request_hash    VARCHAR(255) NOT NULL,
    order_id        VARCHAR(36)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS payments (
    payment_id VARCHAR(36)      PRIMARY KEY,
    order_id   VARCHAR(36)      NOT NULL,
    amount     DOUBLE PRECISION NOT NULL,
    status     VARCHAR(20)      NOT NULL,
    created_at TIMESTAMP        NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_orders_customer ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_auth_tokens_user ON auth_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_payments_order ON payments(order_id);
CREATE INDEX IF NOT EXISTS idx_addresses_user ON addresses(user_id);
