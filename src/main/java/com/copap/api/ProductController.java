package com.copap.api;

import com.copap.api.dto.ProductResponse;
import com.copap.product.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ProductController {

    private final ProductRepository productRepository;
    private final String imageBaseUrl;

    public ProductController(ProductRepository productRepository,
                             @Value("${app.image-base-url:http://localhost:8080}") String imageBaseUrl) {
        this.productRepository = productRepository;
        this.imageBaseUrl = imageBaseUrl;
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductResponse>> getProducts() {
        List<ProductResponse> products = productRepository.findAll().stream()
                .filter(p -> p.isActive())
                .map(p -> {
                    var dto = new ProductResponse();
                    dto.productId = p.getProductId();
                    dto.name = p.getName();
                    dto.price = p.getPrice();
                    // Use stored filename if available, otherwise fall back to default pattern
                    String filename = p.getImageFilename() != null
                            ? p.getImageFilename()
                            : p.getProductId().toLowerCase() + ".jpg";
                    dto.imageUrl = imageBaseUrl + "/images/" + filename;
                    return dto;
                })
                .toList();
        return ResponseEntity.ok(products);
    }
}
