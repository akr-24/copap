package com.copap.api;

import com.copap.api.dto.CreateOrderRequest;
import com.copap.api.exception.OrderNotFoundException;
import com.copap.model.Order;
import com.copap.model.OrderStatus;
import com.copap.product.ProductRepository;
import com.copap.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final ProductRepository productRepository;

    public OrderController(OrderService orderService, ProductRepository productRepository) {
        this.orderService = orderService;
        this.productRepository = productRepository;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request,
            Authentication auth) {

        String customerId = (String) auth.getPrincipal();
        List<String> productIds = request.productIds;

        double totalAmount = productRepository.findByIds(productIds)
                .stream()
                .mapToDouble(p -> p.getPrice())
                .sum();

        String requestHash = customerId + "|" + productIds + "|" + totalAmount;

        Order order = orderService.createOrder(
                idempotencyKey,
                requestHash,
                customerId,
                productIds,
                totalAmount,
                request.addressId
        );

        // Only kick off payment for freshly created orders.
        // Duplicate idempotent requests return the existing order in its
        // current state — trying to re-advance it would fail the state machine.
        if (order.getStatus() == OrderStatus.NEW) {
            orderService.advanceOrder(order.getOrderId(), OrderStatus.PAYMENT_PENDING);
            orderService.startPaymentProcessing(order.getOrderId());
        }

        return ResponseEntity.ok(Map.of(
                "orderId", order.getOrderId(),
                "status", order.getStatus().name(),
                "totalAmount", order.totalAmount()
        ));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listOrders(Authentication auth) {
        String customerId = (String) auth.getPrincipal();
        List<Map<String, Object>> orders = orderService.getOrdersByCustomer(customerId)
                .stream()
                .map(o -> Map.<String, Object>of(
                        "orderId", o.getOrderId(),
                        "status", o.getStatus().name(),
                        "totalAmount", o.totalAmount(),
                        "version", o.getVersion(),
                        "createdAt", o.getCreatedAt().toString()
                ))
                .toList();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrder(
            @PathVariable String orderId,
            Authentication auth) {

        String customerId = (String) auth.getPrincipal();
        Order order = orderService.getOrder(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getCustomerId().equals(customerId)) {
            throw new OrderNotFoundException(orderId);
        }

        return ResponseEntity.ok(Map.of(
                "orderId", order.getOrderId(),
                "status", order.getStatus().name(),
                "totalAmount", order.totalAmount(),
                "version", order.getVersion(),
                "productIds", order.getProductIds(),
                "shippingAddressId", order.getShippingAddressId() != null ? order.getShippingAddressId() : ""
        ));
    }
}
