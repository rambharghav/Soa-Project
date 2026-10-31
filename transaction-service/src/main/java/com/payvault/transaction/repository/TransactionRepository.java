package com.payvault.transaction.repository;

import com.payvault.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findBySenderUserId(Long senderUserId);

    List<Transaction> findByReceiverUserId(Long receiverUserId);
}