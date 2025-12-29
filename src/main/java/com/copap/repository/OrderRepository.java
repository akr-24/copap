package com.copap.repository;

import java.util.Optional;
import com.copap.model.Order;

public interface OrderRepository {
    void save(Order order);
    Optional <Order> findById(String orderId);
    boolean exists(String orderId);
}

