# COPAP Backend

**Concurrent Order Processing and Analytics Platform** — A Spring Boot e-commerce backend demonstrating advanced concurrency patterns, idempotent order processing, and real-time analytics.

> 🔗 **Frontend Repository:** [copap-ui](https://github.com/akr-24/copap-ui)

---

## 🚀 Features

- **Order Processing Engine** — Async order processing with `@Async` + Spring Retry and a state machine
- **Idempotency** — Duplicate request handling for reliable order creation (`Idempotency-Key` header)
- **Optimistic Locking** — Version-based concurrency control on orders
- **Real-time Analytics** — Sliding window analytics for live order/revenue metrics
- **Token Authentication** — Opaque UUID tokens stored in DB, validated per request
- **BCrypt Passwords** — With graceful SHA-256 → BCrypt migration path for existing users
- **Role-based Access Control** — `USER` and `ADMIN` roles enforced via Spring Security `@PreAuthorize`
- **Payment Processing** — Mock payment gateway with retry support and DB-backed dead letter queue
- **Admin Panel API** — Dashboard, analytics, product/order/user management with image uploads
- **Database Migrations** — Flyway versioned migrations; schema managed automatically on startup

---

## 🛠️ Tech Stack

| Concern | Technology |
|---------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.3 |
| Web | Spring MVC (`@RestController`) |
| Security | Spring Security 6 (`TokenAuthenticationFilter`) |
| Database | PostgreSQL 18 |
| JDBC | Spring `JdbcTemplate` |
| Connection Pool | HikariCP (auto-configured by Spring Boot) |
| Migrations | Flyway |
| Async / Retry | Spring `@Async` + Spring Retry (`@Retryable`) |
| JSON | Jackson (via `spring-boot-starter-web`) |
| Build | Maven 3.9+ |

---

## 📋 Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL (any recent version)

---

## 🗄️ Database Setup

Just create an empty database — Flyway runs all migrations automatically on first startup:

```sql
CREATE DATABASE copap;
```

That's it. Flyway creates all tables (`users`, `products`, `orders`, `addresses`, `auth_tokens`, `payments`, `idempotency_records`, `order_items`, `dead_letter_queue`) on first run.

---

## ⚙️ Local Configuration

Copy the example file and fill in your local credentials:

```bash
cp application-local.yml.example application-local.yml
```

Edit `application-local.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/copap
    username: postgres
    password: YOUR_PASSWORD

app:
  cors:
    allowed-origins: http://localhost:5173
  image-base-url: http://localhost:8080
  upload-dir: ./uploads
```

> `application-local.yml` is gitignored and never committed.  
> See `.env.example` for the full list of environment variables used in production.

---

## 🏃 Running Locally

```bash
cd copap-backend
mvn spring-boot:run
```

The `local` Spring profile is activated automatically by the Maven plugin, so `application-local.yml` is picked up without any extra flags.

Server starts at `http://localhost:8080`

---

## 📡 API Endpoints

### Public
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/login` | Login — returns `{ token }` |
| POST | `/register` | Register — returns `{ token }` |
| GET | `/products` | List active products |
| GET | `/images/{filename}` | Serve product image |

### Protected (requires `Authorization: Bearer <token>`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/profile` | Current user info (includes `role`) |
| GET | `/orders` | User's order history |
| POST | `/orders` | Create order (requires `Idempotency-Key` header) |
| GET | `/orders/{id}` | Order details (includes `productIds`) |
| GET | `/addresses` | User's addresses |
| POST | `/addresses` | Create address |
| PUT | `/addresses/{id}` | Update address |
| DELETE | `/addresses/{id}` | Delete address |
| POST | `/addresses/{id}/default` | Set default address |

### Admin (requires `ADMIN` role)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/admin/dashboard` | Stats: orders, revenue, users, products |
| GET | `/admin/analytics` | Sliding window order count and revenue |
| GET | `/admin/products` | All products (includes `imageUrl`) |
| POST | `/admin/products` | Create product |
| PUT | `/admin/products/{id}` | Update product name / price / active |
| DELETE | `/admin/products/{id}` | Soft-deactivate product |
| POST | `/admin/products/{id}/image` | Upload product image (multipart, max 10 MB) |
| GET | `/admin/orders` | All orders |
| GET | `/admin/users` | All users |
| POST | `/admin/recovery/payments` | Re-queue stale PAYMENT_PENDING orders |

---

## 🔐 Authentication

Include the token in every protected request:

```
Authorization: Bearer <token>
```

Tokens expire after **1 hour**. A `401` response means the token is missing or expired — the frontend auto-logs out when this happens.

To promote a user to admin:

```sql
UPDATE users SET role = 'ADMIN' WHERE username = 'your_username';
```

Then log out and back in.

---

## 📊 Order State Machine

```
NEW → PAYMENT_PENDING → PAID → SHIPPED
 ↓          ↓           ↓
FAILED    FAILED      (terminal)
```

Transitions are enforced by `OrderStateMachine`. Async payment processing uses Spring `@Async` + `@Retryable` (3 attempts, exponential backoff). Orders that exhaust all retries go to the `dead_letter_queue` table.

---

## 🏗️ Architecture

```
copap-backend/
├── src/main/java/com/copap/
│   ├── CopapApplication.java       # Entry point (@SpringBootApplication)
│   ├── api/                        # @RestController classes
│   │   ├── exception/              # GlobalExceptionHandler + custom exceptions
│   │   └── dto/                    # Request/response DTOs
│   ├── auth/                       # User, AuthToken, AuthService, JdbcUserRepository
│   ├── analytics/                  # SlidingWindowAnalytics, AnalyticsService (@EventListener)
│   ├── config/                     # SecurityConfig, WebConfig (CORS), AsyncConfig
│   ├── events/                     # OrderPlacedEvent (ApplicationEvent)
│   ├── model/                      # Order, OrderStatus, OrderStateMachine, etc.
│   ├── payment/                    # PaymentService, MockPaymentGateway, JdbcPaymentRepository
│   ├── product/                    # Product, ProductRepository, JdbcProductRepository
│   ├── repository/                 # OrderRepository, CachedOrderRepository, JdbcOrderRepository
│   ├── security/                   # TokenAuthenticationFilter
│   └── service/                    # OrderService, AsyncOrderProcessor, DeadLetterQueueService
└── src/main/resources/
    ├── application.yml             # Base config (reads from env vars)
    ├── application-local.yml       # Local overrides (gitignored)
    └── db/migration/               # Flyway SQL scripts V1–V5
```

---

## 📝 License

MIT License
