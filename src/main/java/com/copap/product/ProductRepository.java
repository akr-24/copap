package com.copap.product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Optional<Product> findById(String productId);

    List<Product> findByIds(List<String> productIds);
}
