package com.payvault.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex
    ) {
        return buildResponse(
                "Bad Request",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex
    ) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Invalid request");

        return buildResponse(
                "Validation Error",
                message,
                HttpStatus.BAD_REQUEST
        );
    }

    private ResponseEntity<Map<String, Object>> buildResponse(
            String error,
            String message,
            HttpStatus status
    ) {
        Map<String, Object> response = new HashMap<>();

        response.put("error", error);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());

        return ResponseEntity
                .status(status)
                .body(response);
    }
}