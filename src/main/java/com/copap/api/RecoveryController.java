package com.copap.api;

import com.copap.payment.PaymentRecoveryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/recovery")
@PreAuthorize("hasRole('ADMIN')")
public class RecoveryController {

    private final PaymentRecoveryService recoveryService;

    public RecoveryController(PaymentRecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }

    @PostMapping("/payments")
    public ResponseEntity<Map<String, String>> recoverPayments() {
        recoveryService.recoverPayments();
        return ResponseEntity.ok(Map.of("message", "Payment recovery triggered"));
    }
}
