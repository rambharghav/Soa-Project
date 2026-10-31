package com.payvault.wallet.repository;

import com.payvault.wallet.entity.LedgerEntry;
import com.payvault.wallet.entity.LedgerEntryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<LedgerEntry> findByWalletIdOrderByCreatedAtDesc(Long walletId);

    Optional<LedgerEntry> findByUserIdAndTransactionRefAndEntryType(
            Long userId,
            String transactionRef,
            LedgerEntryType entryType
    );

    boolean existsByUserIdAndTransactionRefAndEntryType(
            Long userId,
            String transactionRef,
            LedgerEntryType entryType
    );
}
