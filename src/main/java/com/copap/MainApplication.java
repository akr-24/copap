package com.copap;
import com.copap.model.*;
import com.copap.repository.*;

import java.util.List;
public class MainApplication {
    public static void main(String[] args) {

        System.out.println("starting");
        Customer customer = new Customer("C1", "aman", "aman@gmail.com");

        Product product1 = new Product("P1", "phone", 12);
        Product product2 = new Product("P2", "charger", 6);

        Order order = new Order("O1", customer, List.of(product1, product2));

        System.out.println("Order ID: " + order.getOrderId());
        System.out.println("Status: " + order.getStatus());
        System.out.println("Total: " + order.totalAmount());

        OrderRepository repository = new InMemoryOrderRepository();

        repository.save(order);

        repository.findById("O1").ifPresent(o ->
                System.out.println("Found order with status: " + o.getStatus())
        );
    }
}