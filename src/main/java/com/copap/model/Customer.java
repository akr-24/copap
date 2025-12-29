package com.copap.model;

import java.util.Objects;

public class Customer {
    private final String customerId;
    private String name;
    private String email;

    public String getCustomerId() { return customerId; }
    public String getName() { return name; }
    public String getEmail() { return email; }

    public Customer(String customerId, String name, String email) {
        this.customerId = Objects.requireNonNull(customerId);
        this.name = name;
        this.email = email;
    }

    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    // public void setCustomerId(String customerId) {this.customerId = customerId};

    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (!(o instanceof Customer)) return false;
        Customer customer = (Customer) o;
        return Objects.equals(customerId, customer.customerId);
    }

    @Override
    public int hashCode() {
        //  return Objects.hash(customerId);
        return customerId.hashCode();
    }
}