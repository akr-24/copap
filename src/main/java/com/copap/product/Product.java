package com.copap.product;

public class Product {

    private final String productId;
    private final String name;
    private final double price;
    private final boolean active;

    public Product(String productId,
                   String name,
                   double price,
                   boolean active) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.active = active;
    }

    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public boolean isActive() {
        return active;
    }
}
