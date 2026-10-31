package com.payvault.gateway;

import com.payvault.gateway.controller.FallbackController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FallbackControllerTest {

    private FallbackController fallbackController;

    @BeforeEach
    void setUp() {
        fallbackController = new FallbackController();
    }

    @Test
    void authFallbackShouldReturn503ServiceUnavailable() {
        ResponseEntity<Map<String, Object>> response = fallbackController.authFallback();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(503, response.getBody().get("status"));
        assertEquals("Service Unavailable", response.getBody().get("error"));
        assertTrue(((String) response.getBody().get("message")).contains("Auth Service is temporarily unavailable"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void userFallbackShouldReturn503ServiceUnavailable() {
        ResponseEntity<Map<String, Object>> response = fallbackController.userFallback();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(503, response.getBody().get("status"));
        assertTrue(((String) response.getBody().get("message")).contains("User Service is temporarily unavailable"));
    }

    @Test
    void walletFallbackShouldReturn503ServiceUnavailable() {
        ResponseEntity<Map<String, Object>> response = fallbackController.walletFallback();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(503, response.getBody().get("status"));
        assertTrue(((String) response.getBody().get("message")).contains("Wallet Service is temporarily unavailable"));
    }

    @Test
    void transactionFallbackShouldReturn503ServiceUnavailable() {
        ResponseEntity<Map<String, Object>> response = fallbackController.transactionFallback();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(503, response.getBody().get("status"));
        assertTrue(((String) response.getBody().get("message")).contains("Transaction Service is temporarily unavailable"));
    }
}
