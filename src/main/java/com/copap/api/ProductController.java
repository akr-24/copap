package com.copap.api;

import com.copap.api.dto.ProductResponse;
import com.copap.product.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class ProductController implements HttpHandler {

    private static final ObjectMapper mapper = new ObjectMapper();
    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        System.out.println("hitting the product controller");

        try {
            var products = productRepository.findAll()
                    .stream()
                    .map(p -> {
                        var dto = new ProductResponse();
                        dto.productId = p.getProductId();
                        dto.name = p.getName();
                        dto.price = p.getPrice();
                        dto.imageUrl =
                                "http://localhost:8080/images/" +
                                        p.getProductId().toLowerCase() + ".jpg";
                        return dto;
                    })
                    .collect(Collectors.toList());

            byte[] bytes = mapper
                    .writeValueAsString(products)
                    .getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                    .add("Content-Type", "application/json");

            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);

        } catch (Exception e) {
            e.printStackTrace();
            exchange.sendResponseHeaders(500, -1);
        } finally {
            exchange.close();
        }
    }
}