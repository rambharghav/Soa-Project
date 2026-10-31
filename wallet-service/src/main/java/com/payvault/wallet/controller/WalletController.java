package com.payvault.wallet.controller;

import com.payvault.wallet.dto.LedgerEntryResponse;
import com.payvault.wallet.dto.WalletRequest;
import com.payvault.wallet.dto.WalletResponse;
import com.payvault.wallet.service.WalletService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(
            @Valid @RequestBody WalletRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(walletService.createWallet(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WalletResponse> getWalletById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                walletService.getWalletById(id)
        );
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<WalletResponse> getWalletByUserId(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                walletService.getWalletByUserId(userId)
        );
    }

    @GetMapping
    public ResponseEntity<List<WalletResponse>> getAllWallets() {

        return ResponseEntity.ok(
                walletService.getAllWallets()
        );
    }

    @PostMapping("/user/{userId}/credit")
    public ResponseEntity<WalletResponse> credit(
            @PathVariable Long userId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String transactionRef,
            @RequestParam(required = false) String description,
            @RequestHeader(value = "X-Transaction-Ref", required = false) String headerRef,
            @RequestHeader(value = "X-Description", required = false) String headerDesc) {

        String effectiveRef = (transactionRef != null && !transactionRef.isBlank())
                ? transactionRef : headerRef;
        String effectiveDesc = (description != null && !description.isBlank())
                ? description : headerDesc;

        if ((effectiveRef != null && !effectiveRef.isBlank()) ||
            (effectiveDesc != null && !effectiveDesc.isBlank())) {
            return ResponseEntity.ok(
                    walletService.credit(userId, amount, effectiveRef, effectiveDesc)
            );
        }

        return ResponseEntity.ok(
                walletService.credit(userId, amount)
        );
    }

    @PostMapping("/user/{userId}/debit")
    public ResponseEntity<WalletResponse> debit(
            @PathVariable Long userId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String transactionRef,
            @RequestParam(required = false) String description,
            @RequestHeader(value = "X-Transaction-Ref", required = false) String headerRef,
            @RequestHeader(value = "X-Description", required = false) String headerDesc) {

        String effectiveRef = (transactionRef != null && !transactionRef.isBlank())
                ? transactionRef : headerRef;
        String effectiveDesc = (description != null && !description.isBlank())
                ? description : headerDesc;

        if ((effectiveRef != null && !effectiveRef.isBlank()) ||
            (effectiveDesc != null && !effectiveDesc.isBlank())) {
            return ResponseEntity.ok(
                    walletService.debit(userId, amount, effectiveRef, effectiveDesc)
            );
        }

        return ResponseEntity.ok(
                walletService.debit(userId, amount)
        );
    }

    @GetMapping("/user/{userId}/ledger")
    public ResponseEntity<List<LedgerEntryResponse>> getLedgerByUserId(
            @PathVariable Long userId,
            @RequestHeader(value = "X-Auth-User", required = false) String authUser,
            @RequestHeader(value = "X-Auth-Role", required = false) String authRole) {

        return ResponseEntity.ok(
                walletService.getLedgerEntriesByUserId(userId, authUser, authRole)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWallet(
            @PathVariable Long id) {

        walletService.deleteWallet(id);

        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityException(SecurityException ex) {
        Map<String, Object> err = new HashMap<>();
        err.put("timestamp", LocalDateTime.now().toString());
        err.put("status", HttpStatus.FORBIDDEN.value());
        err.put("error", "Forbidden");
        err.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
    }
}