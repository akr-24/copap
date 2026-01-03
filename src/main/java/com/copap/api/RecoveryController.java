package com.copap.api;

import com.copap.payment.PaymentRecoveryService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class RecoveryController implements HttpHandler {

    private final PaymentRecoveryService recoveryService;

    public RecoveryController(PaymentRecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        recoveryService.recoverPayments();
        exchange.sendResponseHeaders(200, -1);
    }
}
