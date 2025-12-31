package com.copap;

import com.copap.model.*;
import com.copap.repository.*;
import com.copap.service.*;
import com.copap.engine.*;
import com.copap.analytics.*;
import com.copap.payment.*;

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


        /** testing idempotency

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


         **/

        /**
         * testing analytics

        SlidingWindowAnalytics analytics =
                new SlidingWindowAnalytics(5 * 60 * 1000);

        DeadLetterQueue dlq = new DeadLetterQueue();
        FailureAwareExecutor executor =
                new FailureAwareExecutor(2, dlq);
        AnalyticsService analyticsService =
                new AnalyticsService(analytics, executor);

        // simulate orders
        analyticsService.publish(new OrderEvent("O1", 500));
        analyticsService.publish(new OrderEvent("O2", 1500));

        try {
            Thread.sleep(1000);
        }catch (InterruptedException e){
            e.printStackTrace();
        }

        System.out.println("Orders (last 5 min): " + analytics.getOrderCount());
        System.out.println("Revenue (last 5 min): " + analytics.getRevenue());

        executor.shutdown();
        **
         *         */
        InMemoryOrderRepository baseRepo = new InMemoryOrderRepository();
        CachedOrderRepository cachedRepo = new CachedOrderRepository(baseRepo);

        OrderRepository orderRepo = cachedRepo;

        IdempotencyRepository idempotencyRepo =
                new InMemoryIdempotencyRepository();

        OrderService service =
                new OrderService(orderRepo, idempotencyRepo);
        Customer customer = new Customer("C1", "Aman", "aman@gmail.com");
        Product product = new Product("P1", "Phone", 50000);
        Order order = new Order("O1", customer, List.of(product));
        orderRepo.save(order);

        order.updateStatus(OrderStatus.VALIDATED);
        order.updateStatus(OrderStatus.INVENTORY_RESERVED);
        order.updateStatus(OrderStatus.PAYMENT_PENDING);
        PaymentGateway gateway = new MockPaymentGateway();
        PaymentRepository paymentRepo = new InMemoryPaymentRepository();
        PaymentService paymentService =
                new PaymentService(gateway, paymentRepo);
        DeadLetterQueue dlq = new DeadLetterQueue();
        FailureAwareExecutor executor = new FailureAwareExecutor(3, dlq);
        executor.submit(
                new OrderProcessingTask(order, paymentService)
        );
        try {
            Thread.sleep(10000);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        executor.shutdown();
    }
}