package com.payvault.wallet.dto;

import com.payvault.wallet.entity.LedgerEntryStatus;
import com.payvault.wallet.entity.LedgerEntryType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LedgerEntryResponse {

    private Long id;
    private Long walletId;
    private Long userId;
    private String transactionRef;
    private LedgerEntryType entryType;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String description;
    private LedgerEntryStatus status;
    private LocalDateTime createdAt;

    public LedgerEntryResponse() {
    }

    public LedgerEntryResponse(
            Long id,
            Long walletId,
            Long userId,
            String transactionRef,
            LedgerEntryType entryType,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String description,
            LedgerEntryStatus status,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.walletId = walletId;
        this.userId = userId;
        this.transactionRef = transactionRef;
        this.entryType = entryType;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public LedgerEntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(LedgerEntryType entryType) {
        this.entryType = entryType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getBalanceBefore() {
        return balanceBefore;
    }

    public void setBalanceBefore(BigDecimal balanceBefore) {
        this.balanceBefore = balanceBefore;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LedgerEntryStatus getStatus() {
        return status;
    }

    public void setStatus(LedgerEntryStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
