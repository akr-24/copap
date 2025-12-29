package com.copap;

import com.copap.model.*;
import com.copap.repository.*;
import com.copap.service.*;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainApplication {
    public static void main(String[] args) {

        System.out.println("starting");
        Customer customer = new Customer("C1", "aman", "aman@gmail.com");

        Product product1 = new Product("P1", "phone", 12);
        Product product2 = new Product("P2", "charger", 6);
        Product p = new Product("P", "adapter", 9);

        Order order = new Order("O1", customer, List.of(product1, product2));
//
//        System.out.println("Order ID: " + order.getOrderId());
//        System.out.println("Status: " + order.getStatus());
//        System.out.println("Total: " + order.totalAmount());

        OrderRepository repository = new InMemoryOrderRepository();
        OrderService service = new OrderService(repository);
//        repository.save(order);
        service.createOrder(order);
//
//        repository.findById("O1").ifPresent(o ->
//                System.out.println("Found order with status: " + o.getStatus())
//        );

        // concurrency testing
//        ExecutorService executor = Executors.newFixedThreadPool(5);
//
//        for (int i = 0; i < 10; i++) {
//            executor.submit(() -> {
//                Order order = new Order("O1", customer, List.of(p));
//                try {
//                    service.createOrder(order);
//                    System.out.println("Order created by " + Thread.currentThread().getName());
//                } catch (Exception e) {
//                    System.out.println("Failed by " + Thread.currentThread().getName());
//                }
//            });
//        }
//
//        executor.shutdown();

        System.out.println("Initial: " + order.getStatus());

        service.advanceOrder("O1", OrderStatus.VALIDATED);
        System.out.println("After VALIDATED: " + order.getStatus());

        service.advanceOrder("O1", OrderStatus.PAID); // ❌ illegal

    }
}