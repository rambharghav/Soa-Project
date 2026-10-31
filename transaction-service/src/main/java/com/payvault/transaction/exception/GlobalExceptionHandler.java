package com.payvault.transaction.exception;

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

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTransactionNotFound(
            TransactionNotFoundException ex
    ) {
        return buildResponse(
                "Transaction Not Found",
                ex.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(InvalidTransferException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidTransfer(
            InvalidTransferException ex
    ) {
        return buildResponse(
                "Invalid Transfer",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleServiceUnavailable(
            ServiceUnavailableException ex
    ) {
        return buildResponse(
                "Service Unavailable",
                ex.getMessage(),
                HttpStatus.SERVICE_UNAVAILABLE
        );
    }

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(
            Exception ex
    ) {
        return buildResponse(
                "Internal Server Error",
                "Something went wrong while processing the request",
                HttpStatus.INTERNAL_SERVER_ERROR
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