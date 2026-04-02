package com.copap.product;

public class Product {

    private final String productId;
    private final String name;
    private final double price;
    private final boolean active;
    private final String imageFilename;

    public Product(String productId, String name, double price, boolean active) {
        this(productId, name, price, active, null);
    }

    public Product(String productId, String name, double price, boolean active, String imageFilename) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.active = active;
        this.imageFilename = imageFilename;
    }

    public String getProductId()    { return productId; }
    public String getName()         { return name; }
    public double getPrice()        { return price; }
    public boolean isActive()       { return active; }
    public String getImageFilename(){ return imageFilename; }
}
