# COPAP Backend

**Concurrent Order Processing and Analytics Platform** - A high-performance e-commerce backend built with raw Java demonstrating advanced concurrency patterns.

> 🔗 **Frontend Repository:** [copap-ui](https://github.com/akr-24/copap-ui)

## 🚀 Features

- **Order Processing Engine** - Async order processing with state machine
- **Idempotency** - Duplicate request handling for reliable order creation
- **Optimistic Locking** - Safe concurrent order updates
- **Real-time Analytics** - Sliding window analytics for order metrics
- **JWT Authentication** - Secure token-based authentication
- **Role-based Access Control** - Admin and User roles
- **Payment Processing** - Mock payment gateway with retry support
- **Dead Letter Queue** - Failed task handling for recovery

## 🛠️ Tech Stack

- **Language:** Java 17+
- **Server:** JDK HttpServer (no framework)
- **Database:** PostgreSQL
- **Connection Pool:** HikariCP
- **JSON:** Gson

## 📋 Prerequisites

- Java 17 or later
- PostgreSQL 12+
- Maven (optional, for dependency management)

## 🗄️ Database Setup

1. Create the database:
```sql
CREATE DATABASE copap;
```

2. Run the schema:
```sql
-- Users table
CREATE TABLE users (
    user_id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    role VARCHAR(20) DEFAULT 'USER'
);

-- Products table
CREATE TABLE products (
    product_id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    active BOOLEAN DEFAULT true
);

-- Orders table
CREATE TABLE orders (
    order_id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) REFERENCES users(user_id),
    product_ids TEXT,
    status VARCHAR(20) NOT NULL,
    total_amount DECIMAL(10,2),
    version BIGINT DEFAULT 0,
    shipping_address_id VARCHAR(36),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Addresses table
CREATE TABLE addresses (
    address_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(user_id),
    label VARCHAR(50) DEFAULT 'Home',
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    street VARCHAR(500) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100),
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(100) DEFAULT 'India',
    is_default BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Idempotency keys table
CREATE TABLE idempotency_keys (
    idempotency_key VARCHAR(255) PRIMARY KEY,
    request_hash VARCHAR(255),
    order_id VARCHAR(36),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Auth tokens table
CREATE TABLE auth_tokens (
    token VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(36) REFERENCES users(user_id),
    expires_at TIMESTAMP NOT NULL
);

-- Sample products
INSERT INTO products (product_id, name, price) VALUES
    ('P1', 'Laptop', 50000),
    ('P2', 'Smartphone', 25000),
    ('P3', 'Headphones', 5000);
```

## ⚙️ Configuration

Update database credentials in `MainApplication.java`:
```java
config.setJdbcUrl("jdbc:postgresql://localhost:5432/copap");
config.setUsername("postgres");
config.setPassword("your_password");
```

## 🏃 Running the Server

```bash
# Navigate to backend directory
cd copap-backend

# Compile
javac -cp "lib/*" -d out $(find src -name "*.java")

# Run (Linux/Mac)
java -cp "out:lib/*" com.copap.MainApplication

# Run (Windows)
java -cp "out;lib/*" com.copap.MainApplication
```

Server starts at `http://localhost:8080`

## 📡 API Endpoints

### Public Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/login` | User login |
| POST | `/register` | User registration |
| GET | `/products` | List all products |
| GET | `/images/{name}` | Get product image |

### Protected Endpoints (Require Auth Token)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/profile` | Get user profile |
| GET | `/orders` | Get user's orders |
| POST | `/orders` | Create order (requires Idempotency-Key header) |
| GET | `/orders/{id}` | Get order details |
| GET | `/addresses` | Get user's addresses |
| POST | `/addresses` | Create address |
| PUT | `/addresses/{id}` | Update address |
| DELETE | `/addresses/{id}` | Delete address |

### Admin Endpoints (Require ADMIN role)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/admin/dashboard` | Dashboard statistics |
| GET | `/admin/analytics` | Real-time analytics |
| GET | `/admin/products` | All products |
| POST | `/admin/products` | Create product |
| PUT | `/admin/products/{id}` | Update product |
| DELETE | `/admin/products/{id}` | Deactivate product |
| GET | `/admin/orders` | All orders |
| GET | `/admin/users` | All users |

## 🔐 Authentication

Include the auth token in requests:
```
Authorization: Bearer <token>
```

## 📊 Order State Machine

```
NEW → VALIDATED → INVENTORY_RESERVED → PAYMENT_PENDING → PAID → SHIPPED
  ↓       ↓              ↓                   ↓
FAILED  FAILED        FAILED              FAILED
```

## 🏗️ Architecture Highlights

- **No Framework** - Pure Java implementation for learning
- **Async Processing** - Custom `FailureAwareExecutor` for background tasks
- **State Machine** - Type-safe order state transitions
- **Optimistic Locking** - Version-based concurrency control
- **Idempotency** - Request deduplication for reliability
- **Sliding Window Analytics** - Real-time metrics with time-bucketed data

## 📁 Project Structure

```
copap-backend/
├── src/main/java/com/copap/
│   ├── api/              # HTTP handlers (controllers)
│   ├── auth/             # Authentication & authorization
│   ├── analytics/        # Real-time analytics
│   ├── engine/           # Order processing engine
│   ├── model/            # Domain models
│   ├── payment/          # Payment processing
│   ├── repository/       # Data access layer
│   └── MainApplication.java
├── lib/                  # Dependencies (JARs)
└── static/images/        # Product images
```

## 📝 License

MIT License

