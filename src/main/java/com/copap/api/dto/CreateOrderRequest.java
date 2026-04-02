package com.copap.api.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class CreateOrderRequest {
    @NotEmpty
    public List<String> productIds;
    public String addressId;
}