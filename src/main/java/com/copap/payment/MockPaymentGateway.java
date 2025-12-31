package com.copap.payment;

import java.util.Random;
public class MockPaymentGateway implements PaymentGateway {

    private final Random random = new Random();
    @Override
    public PaymentResult charge(String paymentId, String orderId, double amount){
        int outcome = random.nextInt(10);
        System.out.println("this part is being hti");
        if (outcome < 6) {
            return PaymentResult.SUCCESS;
        } else if (outcome < 8) {
            return PaymentResult.RETRYABLE_FAILURE;
        } else {
            return PaymentResult.FAILED;
        }
    }
}