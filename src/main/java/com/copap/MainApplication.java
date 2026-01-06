package com.copap;

import com.copap.analytics.SlidingWindowAnalytics;
import com.copap.api.*;
import com.copap.auth.*;
import com.copap.db.TransactionManager;
import com.copap.engine.DeadLetterQueue;
import com.copap.engine.FailureAwareExecutor;
import com.copap.payment.*;
import com.copap.product.JdbcProductRepository;
import com.copap.product.ProductRepository;
import com.copap.repository.*;
import com.copap.service.OrderService;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.concurrent.Executors;

/**
 * COPAP - Concurrent Order Processing and Analytics Platform
 * 
 * Main entry point for the backend server.
 * Initializes all components and starts the HTTP server on port 8080.
 */
public class MainApplication {

    // ============================================
    // CONFIGURATION - Update these values
    // ============================================
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/copap";
    private static final String DB_USER = "ur_user";
    private static final String DB_PASSWORD = "ur_username";
    private static final int SERVER_PORT = 8080;
    private static final int EXECUTOR_POOL_SIZE = 3;
    private static final long ANALYTICS_WINDOW_MS = 5 * 60 * 1000; // 5 minutes

    public static void main(String[] args) {
        try {
            System.out.println("╔════════════════════════════════════════════════════════╗");
            System.out.println("║     COPAP - Concurrent Order Processing Platform       ║");
            System.out.println("╚════════════════════════════════════════════════════════╝");
            System.out.println();

            // ============================================
            // 1. DATABASE CONNECTION
            // ============================================
            System.out.println("[1/7] Connecting to database...");
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("    ✓ Database connected successfully");

            // ============================================
            // 2. REPOSITORIES (Data Access Layer)
            // ============================================
            System.out.println("[2/7] Initializing repositories...");
            
            // User & Auth repositories
            JdbcUserRepository userRepository = new JdbcUserRepository(connection);
            AuthTokenRepository authTokenRepository = new JdbcAuthTokenRepository(connection);
            
            // Order repositories with caching
            JdbcOrderRepository jdbcOrderRepository = new JdbcOrderRepository(connection);
            CachedOrderRepository cachedOrderRepository = new CachedOrderRepository(jdbcOrderRepository);
            OrderRepository orderRepository = cachedOrderRepository;
            
            // Idempotency repository
            IdempotencyRepository idempotencyRepository = new JdbcIdempotencyRepository(connection);
            
            // Product repository
            ProductRepository productRepository = new JdbcProductRepository(connection);
            
            // Payment repository
            PaymentRepository paymentRepository = new JdbcPaymentRepository(connection);
            
            System.out.println("    ✓ All repositories initialized");

            // ============================================
            // 3. SERVICES (Business Logic Layer)
            // ============================================
            System.out.println("[3/7] Initializing services...");
            
            // Transaction manager for ACID operations
            TransactionManager transactionManager = new TransactionManager(connection);
            
            // Authentication service
            AuthService authService = new AuthService(userRepository, authTokenRepository);
            
            // Payment service with mock gateway
            PaymentGateway paymentGateway = new MockPaymentGateway();
            PaymentService paymentService = new PaymentService(paymentGateway, paymentRepository);
            
            // Async execution engine with dead letter queue
            DeadLetterQueue dlq = new DeadLetterQueue();
            FailureAwareExecutor executor = new FailureAwareExecutor(EXECUTOR_POOL_SIZE, dlq);
            
            // Order service (core business logic)
            OrderService orderService = new OrderService(
                    orderRepository,
                    idempotencyRepository,
                    paymentService,
                    transactionManager,
                    executor
            );
            
            // Analytics service
            SlidingWindowAnalytics analytics = new SlidingWindowAnalytics(ANALYTICS_WINDOW_MS);
            
            System.out.println("    ✓ All services initialized");

            // ============================================
            // 4. HTTP SERVER SETUP
            // ============================================
            System.out.println("[4/7] Creating HTTP server...");
            HttpServer server = HttpServer.create(new InetSocketAddress(SERVER_PORT), 0);
            System.out.println("    ✓ Server created on port " + SERVER_PORT);

            // ============================================
            // 5. CONTROLLER INSTANTIATION
            // ============================================
            System.out.println("[5/7] Initializing controllers...");
            
            // Auth controllers
            LoginController loginController = new LoginController(authService);
            RegisterController registerController = new RegisterController(userRepository, authService);
            ProfileController profileController = new ProfileController(authTokenRepository, connection);
            
            // Product controller
            ProductController productController = new ProductController(productRepository);
            
            // Order controller
            OrderController orderController = new OrderController(
                    orderService,
                    orderRepository,
                    productRepository,
                    paymentService,
                    transactionManager,
                    executor,
                    authTokenRepository
            );
            
            // Address controller
            AddressController addressController = new AddressController(authTokenRepository, connection);
            
            // Admin controller
            AdminController adminController = new AdminController(
                    authTokenRepository,
                    userRepository,
                    productRepository,
                    orderRepository,
                    analytics,
                    connection
            );
            
            // Image controller
            ImageController imageController = new ImageController();
            
            System.out.println("    ✓ All controllers initialized");

            // ============================================
            // 6. ROUTE REGISTRATION WITH FILTERS
            // ============================================
            System.out.println("[6/7] Registering routes...");
            
            // Filters
            CorsFilter corsFilter = new CorsFilter();
            AuthFilter authFilter = new AuthFilter(authTokenRepository);
            
            // ------------------------------------------
            // PUBLIC ROUTES (CORS only)
            // ------------------------------------------
            HttpContext loginCtx = server.createContext("/login", loginController);
            loginCtx.getFilters().add(corsFilter);
            
            HttpContext registerCtx = server.createContext("/register", registerController);
            registerCtx.getFilters().add(corsFilter);
            
            HttpContext productsCtx = server.createContext("/products", productController);
            productsCtx.getFilters().add(corsFilter);
            
            HttpContext imagesCtx = server.createContext("/images", imageController);
            imagesCtx.getFilters().add(corsFilter);
            
            // ------------------------------------------
            // PROTECTED ROUTES (CORS only - auth handled internally by controllers)
            // ------------------------------------------
            HttpContext profileCtx = server.createContext("/profile", profileController);
            profileCtx.getFilters().add(corsFilter);
            
            HttpContext ordersCtx = server.createContext("/orders", orderController);
            ordersCtx.getFilters().add(corsFilter);
            
            HttpContext addressesCtx = server.createContext("/addresses", addressController);
            addressesCtx.getFilters().add(corsFilter);
            
            // ------------------------------------------
            // ADMIN ROUTES (CORS + Auth checked internally)
            // ------------------------------------------
            HttpContext adminCtx = server.createContext("/admin", adminController);
            adminCtx.getFilters().add(corsFilter);
            
            System.out.println("    ✓ Routes registered:");
            System.out.println("      PUBLIC:");
            System.out.println("        • POST /login           - User login");
            System.out.println("        • POST /register        - User registration");
            System.out.println("        • GET  /products        - List products");
            System.out.println("        • GET  /images/{name}   - Product images");
            System.out.println("      PROTECTED (auth handled by controller):");
            System.out.println("        • GET  /profile         - User profile");
            System.out.println("        • GET  /orders          - List orders");
            System.out.println("        • POST /orders          - Create order");
            System.out.println("        • GET  /orders/{id}     - Get order details");
            System.out.println("        • CRUD /addresses       - Manage addresses");
            System.out.println("      ADMIN (requires ADMIN role):");
            System.out.println("        • GET  /admin/dashboard - Dashboard stats");
            System.out.println("        • GET  /admin/analytics - Real-time analytics");
            System.out.println("        • CRUD /admin/products  - Manage products");
            System.out.println("        • GET  /admin/orders    - All orders");
            System.out.println("        • GET  /admin/users     - All users");

            // ============================================
            // 7. START SERVER
            // ============================================
            System.out.println("[7/7] Starting server...");
            server.setExecutor(Executors.newFixedThreadPool(10));
            server.start();
            
            System.out.println();
            System.out.println("╔════════════════════════════════════════════════════════╗");
            System.out.println("║          Server started successfully!                  ║");
            System.out.println("║          Listening on http://localhost:" + SERVER_PORT + "            ║");
            System.out.println("╚════════════════════════════════════════════════════════╝");
            System.out.println();
            System.out.println("Press Ctrl+C to stop the server...");

            // ============================================
            // SHUTDOWN HOOK
            // ============================================
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down...");
                server.stop(0);
                executor.shutdown();
                try {
                    connection.close();
                } catch (Exception e) {
                    // Ignore
                }
                System.out.println("Goodbye!");
            }));

        } catch (Exception e) {
            System.err.println("╔════════════════════════════════════════════════════════╗");
            System.err.println("║              FAILED TO START SERVER                    ║");
            System.err.println("╚════════════════════════════════════════════════════════╝");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
