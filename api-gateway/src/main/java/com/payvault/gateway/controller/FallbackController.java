package com.payvault.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/auth")
    public ResponseEntity<Map<String, Object>> authFallback() {
        return buildFallbackResponse("Auth Service is temporarily unavailable. Please try again later.");
    }

    @RequestMapping("/users")
    public ResponseEntity<Map<String, Object>> userFallback() {
        return buildFallbackResponse("User Service is temporarily unavailable. Please try again later.");
    }

    @RequestMapping("/wallets")
    public ResponseEntity<Map<String, Object>> walletFallback() {
        return buildFallbackResponse("Wallet Service is temporarily unavailable. Please try again later.");
    }

    @RequestMapping("/transactions")
    public ResponseEntity<Map<String, Object>> transactionFallback() {
        return buildFallbackResponse("Transaction Service is temporarily unavailable. Please try again later.");
    }

    private ResponseEntity<Map<String, Object>> buildFallbackResponse(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        body.put("error", "Service Unavailable");
        body.put("message", message);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
