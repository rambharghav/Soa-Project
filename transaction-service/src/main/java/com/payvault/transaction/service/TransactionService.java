package com.payvault.transaction.service;

import com.payvault.transaction.dto.TransactionResponse;
import com.payvault.transaction.dto.TransferRequest;

import java.util.List;

public interface TransactionService {

    TransactionResponse transfer(TransferRequest request);

    TransactionResponse getTransactionById(Long id);

    List<TransactionResponse> getAllTransactions();

    List<TransactionResponse> getUserTransactions(Long userId);
}