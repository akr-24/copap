package com.copap.api;

import com.copap.analytics.AnalyticsService;
import com.copap.product.JdbcProductRepository;
import com.copap.repository.OrderRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final JdbcTemplate jdbcTemplate;
    private final JdbcProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final AnalyticsService analyticsService;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${app.image-base-url:http://localhost:8080}")
    private String imageBaseUrl;

    public AdminController(JdbcTemplate jdbcTemplate,
                           JdbcProductRepository productRepository,
                           OrderRepository orderRepository,
                           AnalyticsService analyticsService) {
        this.jdbcTemplate = jdbcTemplate;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.analyticsService = analyticsService;
    }

    @PostConstruct
    public void init() {
        try {
            Path imagesDir = Paths.get(uploadDir).toAbsolutePath().resolve("images");
            Files.createDirectories(imagesDir);
            System.out.println("[AdminController] Upload directory ready: " + imagesDir);
        } catch (IOException e) {
            System.err.println("[AdminController] WARNING: Could not create upload directory: " + e.getMessage());
        }
    }

    private String imageUrlFor(com.copap.product.Product p) {
        String filename = p.getImageFilename() != null
                ? p.getImageFilename()
                : p.getProductId().toLowerCase() + ".jpg";
        return imageBaseUrl + "/images/" + filename;
    }

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics() {
        return ResponseEntity.ok(Map.of(
                "windowOrderCount", analyticsService.getOrderCount(),
                "windowRevenue",    analyticsService.getRevenue(),
                "timestamp",        Instant.now().toString()
        ));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        long totalOrders = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders", Long.class);
        double totalRevenue = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE status = 'PAID'", Double.class);
        long totalUsers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users", Long.class);
        long totalProducts = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM products", Long.class);

        Map<String, Long> statusMap = new HashMap<>();
        jdbcTemplate.query(
                "SELECT status, COUNT(*) as count FROM orders GROUP BY status",
                (rs) -> {
                    statusMap.put(rs.getString("status"), rs.getLong("count"));
                }
        );

        var recentOrders = jdbcTemplate.query(
                "SELECT order_id, status, total_amount, created_at FROM orders ORDER BY created_at DESC LIMIT 10",
                (rs, rowNum) -> Map.<String, Object>of(
                        "orderId",     rs.getString("order_id"),
                        "status",      rs.getString("status"),
                        "totalAmount", rs.getDouble("total_amount"),
                        "createdAt",   rs.getTimestamp("created_at").toString()
                )
        );

        return ResponseEntity.ok(Map.of(
                "totalOrders",    totalOrders,
                "totalRevenue",   totalRevenue,
                "totalUsers",     totalUsers,
                "totalProducts",  totalProducts,
                "ordersByStatus", statusMap,
                "recentOrders",   recentOrders,
                "windowAnalytics", Map.of(
                        "orderCount", analyticsService.getOrderCount(),
                        "revenue",    analyticsService.getRevenue()
                )
        ));
    }

    @GetMapping("/products")
    public ResponseEntity<List<Map<String, Object>>> getProducts() {
        var products = productRepository.findAll().stream()
                .map(p -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("productId", p.getProductId());
                    m.put("name",      p.getName());
                    m.put("price",     p.getPrice());
                    m.put("active",    p.isActive());
                    m.put("imageUrl",  imageUrlFor(p));
                    return m;
                })
                .toList();
        return ResponseEntity.ok(products);
    }

    @PostMapping("/products")
    public ResponseEntity<Map<String, Object>> createProduct(@RequestBody Map<String, Object> body) {
        String productId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String name = (String) body.get("name");
        double price = ((Number) body.get("price")).doubleValue();
        productRepository.save(productId, name, price);

        Map<String, Object> resp = new HashMap<>();
        resp.put("productId", productId);
        resp.put("name",      name);
        resp.put("price",     price);
        resp.put("active",    true);
        resp.put("imageUrl",  imageBaseUrl + "/images/" + productId.toLowerCase() + ".jpg");
        return ResponseEntity.status(201).body(resp);
    }

    @PutMapping("/products/{productId}")
    public ResponseEntity<Map<String, Boolean>> updateProduct(
            @PathVariable String productId,
            @RequestBody Map<String, Object> body) {

        String name    = (String) body.get("name");
        Double price   = body.containsKey("price") ? ((Number) body.get("price")).doubleValue() : null;
        Boolean active = (Boolean) body.get("active");
        productRepository.update(productId, name, price, active);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<Map<String, Boolean>> deleteProduct(@PathVariable String productId) {
        productRepository.softDelete(productId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping(value = "/products/{productId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadProductImage(
            @PathVariable String productId,
            @RequestParam("image") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No file provided"));
        }

        String originalName = file.getOriginalFilename();
        String ext = (originalName != null && originalName.contains("."))
                ? originalName.substring(originalName.lastIndexOf('.')).toLowerCase()
                : ".jpg";

        String filename = productId.toLowerCase() + ext;

        // Use absolute path to avoid working-directory resolution issues
        Path imagesDir = Paths.get(uploadDir).toAbsolutePath().resolve("images");
        Files.createDirectories(imagesDir);
        Path dest = imagesDir.resolve(filename);

        // Files.copy with InputStream is reliable regardless of temp file location
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

        productRepository.updateImageFilename(productId, filename);

        String imageUrl = imageBaseUrl + "/images/" + filename;
        System.out.println("[AdminController] Image saved: " + dest + " → " + imageUrl);
        return ResponseEntity.ok(Map.of("imageUrl", imageUrl, "filename", filename));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Map<String, Object>>> getAllOrders() {
        var orders = jdbcTemplate.query(
                "SELECT order_id, customer_id, status, total_amount, version, created_at FROM orders ORDER BY created_at DESC",
                (rs, rowNum) -> Map.<String, Object>of(
                        "orderId",     rs.getString("order_id"),
                        "customerId",  rs.getString("customer_id"),
                        "status",      rs.getString("status"),
                        "totalAmount", rs.getDouble("total_amount"),
                        "version",     rs.getLong("version"),
                        "createdAt",   rs.getTimestamp("created_at").toString()
                )
        );
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getUsers() {
        var users = jdbcTemplate.query(
                "SELECT user_id, username, email, role FROM users",
                (rs, rowNum) -> Map.<String, Object>of(
                        "userId",   rs.getString("user_id"),
                        "username", rs.getString("username"),
                        "email",    rs.getString("email") != null ? rs.getString("email") : "",
                        "role",     rs.getString("role") != null ? rs.getString("role") : "USER"
                )
        );
        return ResponseEntity.ok(users);
    }
}
