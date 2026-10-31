package com.payvault.transaction.controller;

import com.payvault.transaction.dto.TransactionResponse;
import com.payvault.transaction.dto.TransferRequest;
import com.payvault.transaction.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(transactionService.transfer(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransactionById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                transactionService.getTransactionById(id)
        );
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAllTransactions() {
        return ResponseEntity.ok(
                transactionService.getAllTransactions()
        );
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TransactionResponse>> getUserTransactions(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(
                transactionService.getUserTransactions(userId)
        );
    }
}