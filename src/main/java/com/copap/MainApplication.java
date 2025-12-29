package com.copap;

import com.copap.model.*;
import com.copap.repository.*;
import com.copap.service.*;
import com.copap.engine.*;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainApplication {
    public static void main(String[] args) {

        System.out.println("starting");

        /**
         * state - testing
        System.out.println("Initial: " + order.getStatus());

        service.advanceOrder("O1", OrderStatus.VALIDATED);
        System.out.println("After VALIDATED: " + order.getStatus());

        service.advanceOrder("O1", OrderStatus.PAID); // will through exception

         **/

        IdempotencyRepository idemRepo = new InMemoryIdempotencyRepository();
        OrderRepository orderRepo = new InMemoryOrderRepository();

        OrderService service = new OrderService(orderRepo, idemRepo);

        Customer customer = new Customer("C1", "Aman", "aman@gmail.com");
        Product p = new Product("P1", "Phone", 50000);

        String idemKey = "REQ-123";

        Order o1 = service.createOrder(idemKey, customer, List.of(p));
        Order o2 = service.createOrder(idemKey, customer, List.of(p));

        System.out.println(o1.getOrderId());
        System.out.println(o2.getOrderId());


        DeadLetterQueue dlq = new DeadLetterQueue();
        FailureAwareExecutor executor =
                new FailureAwareExecutor(3, dlq);

        executor.submit(new OrderProcessingTask(o1));

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // best practice
        }

        System.out.println("DLQ size: " + dlq.size());
        executor.shutdown();

    }
}