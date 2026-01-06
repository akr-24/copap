package com.copap.api.dto;

import java.util.List;

public class CreateOrderRequest {
    public String customerId;
    public List<String> productIds;
    public String addressId;  // Shipping address ID
}