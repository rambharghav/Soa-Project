package com.payvault.wallet.service;

import com.payvault.wallet.dto.LedgerEntryResponse;
import com.payvault.wallet.dto.WalletRequest;
import com.payvault.wallet.dto.WalletResponse;

import java.math.BigDecimal;
import java.util.List;

public interface WalletService {

    WalletResponse createWallet(WalletRequest request);

    WalletResponse getWalletById(Long id);

    WalletResponse getWalletByUserId(Long userId);

    List<WalletResponse> getAllWallets();

    WalletResponse credit(Long userId, BigDecimal amount);

    WalletResponse credit(Long userId, BigDecimal amount, String transactionRef, String description);

    WalletResponse debit(Long userId, BigDecimal amount);

    WalletResponse debit(Long userId, BigDecimal amount, String transactionRef, String description);

    void deleteWallet(Long id);

    List<LedgerEntryResponse> getLedgerEntriesByUserId(Long userId);

    List<LedgerEntryResponse> getLedgerEntriesByUserId(Long userId, String authUser, String authRole);
}