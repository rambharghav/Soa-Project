package com.payvault.wallet.config;

import com.payvault.wallet.entity.LedgerEntry;
import com.payvault.wallet.entity.LedgerEntryStatus;
import com.payvault.wallet.entity.LedgerEntryType;
import com.payvault.wallet.entity.Wallet;
import com.payvault.wallet.repository.LedgerEntryRepository;
import com.payvault.wallet.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class LedgerHistoricalMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LedgerHistoricalMigrationRunner.class);

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerHistoricalMigrationRunner(
            WalletRepository walletRepository,
            LedgerEntryRepository ledgerEntryRepository
    ) {
        this.walletRepository = walletRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Override
    public void run(String... args) {
        if (ledgerEntryRepository.count() > 0) {
            return;
        }

        log.info("Populating initial baseline ledger entries for existing accounts...");

        // User 1: Current balance 500.00
        Optional<Wallet> w1 = walletRepository.findByUserId(1L);
        if (w1.isPresent()) {
            Wallet wallet = w1.get();
            LocalDateTime t0 = LocalDateTime.now().minusDays(30);

            saveEntry(wallet.getId(), 1L, "INIT-1", LedgerEntryType.CREDIT,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    "Account opened", LedgerEntryStatus.POSTED, t0);

            saveEntry(wallet.getId(), 1L, "DEP-1001", LedgerEntryType.CREDIT,
                    new BigDecimal("5000.00"), BigDecimal.ZERO, new BigDecimal("5000.00"),
                    "Initial Wallet Funding", LedgerEntryStatus.POSTED, t0.plusHours(1));

            saveEntry(wallet.getId(), 1L, "TX-1", LedgerEntryType.DEBIT,
                    new BigDecimal("1000.00"), new BigDecimal("5000.00"), new BigDecimal("4000.00"),
                    "Transfer to User #2", LedgerEntryStatus.POSTED, t0.plusDays(5));

            saveEntry(wallet.getId(), 1L, "TX-2", LedgerEntryType.DEBIT,
                    new BigDecimal("1000.00"), new BigDecimal("4000.00"), new BigDecimal("3000.00"),
                    "Transfer to User #3", LedgerEntryStatus.POSTED, t0.plusDays(6));

            saveEntry(wallet.getId(), 1L, "TX-3", LedgerEntryType.DEBIT,
                    new BigDecimal("2000.00"), new BigDecimal("3000.00"), new BigDecimal("1000.00"),
                    "Transfer to User #3", LedgerEntryStatus.POSTED, t0.plusDays(10));

            saveEntry(wallet.getId(), 1L, "TX-4", LedgerEntryType.DEBIT,
                    new BigDecimal("500.00"), new BigDecimal("1000.00"), new BigDecimal("500.00"),
                    "Transfer to User #2", LedgerEntryStatus.POSTED, t0.plusDays(15));
        }

        // User 2: Current balance 3000.00
        Optional<Wallet> w2 = walletRepository.findByUserId(2L);
        if (w2.isPresent()) {
            Wallet wallet = w2.get();
            LocalDateTime t0 = LocalDateTime.now().minusDays(30);

            saveEntry(wallet.getId(), 2L, "INIT-2", LedgerEntryType.CREDIT,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    "Account opened", LedgerEntryStatus.POSTED, t0);

            saveEntry(wallet.getId(), 2L, "TX-1", LedgerEntryType.CREDIT,
                    new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("1000.00"),
                    "Transfer from User #1", LedgerEntryStatus.POSTED, t0.plusDays(5));

            saveEntry(wallet.getId(), 2L, "TX-4", LedgerEntryType.CREDIT,
                    new BigDecimal("500.00"), new BigDecimal("1000.00"), new BigDecimal("1500.00"),
                    "Transfer from User #1", LedgerEntryStatus.POSTED, t0.plusDays(15));

            saveEntry(wallet.getId(), 2L, "TX-5", LedgerEntryType.CREDIT,
                    new BigDecimal("500.00"), new BigDecimal("1500.00"), new BigDecimal("2000.00"),
                    "Transfer from User #4", LedgerEntryStatus.POSTED, t0.plusDays(16));

            saveEntry(wallet.getId(), 2L, "DEP-1002", LedgerEntryType.CREDIT,
                    new BigDecimal("1000.00"), new BigDecimal("2000.00"), new BigDecimal("3000.00"),
                    "Wallet Top-up", LedgerEntryStatus.POSTED, t0.plusDays(17));
        }

        // User 3: Current balance 3000.00
        Optional<Wallet> w3 = walletRepository.findByUserId(3L);
        if (w3.isPresent()) {
            Wallet wallet = w3.get();
            LocalDateTime t0 = LocalDateTime.now().minusDays(30);

            saveEntry(wallet.getId(), 3L, "INIT-3", LedgerEntryType.CREDIT,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    "Account opened", LedgerEntryStatus.POSTED, t0);

            saveEntry(wallet.getId(), 3L, "TX-2", LedgerEntryType.CREDIT,
                    new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("1000.00"),
                    "Transfer from User #1", LedgerEntryStatus.POSTED, t0.plusDays(6));

            saveEntry(wallet.getId(), 3L, "TX-3", LedgerEntryType.CREDIT,
                    new BigDecimal("2000.00"), new BigDecimal("1000.00"), new BigDecimal("3000.00"),
                    "Transfer from User #1", LedgerEntryStatus.POSTED, t0.plusDays(10));
        }

        // User 4: Current balance 2500.00
        Optional<Wallet> w4 = walletRepository.findByUserId(4L);
        if (w4.isPresent()) {
            Wallet wallet = w4.get();
            LocalDateTime t0 = LocalDateTime.now().minusDays(30);

            saveEntry(wallet.getId(), 4L, "INIT-4", LedgerEntryType.CREDIT,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    "Account opened", LedgerEntryStatus.POSTED, t0);

            saveEntry(wallet.getId(), 4L, "DEP-1004", LedgerEntryType.CREDIT,
                    new BigDecimal("3000.00"), BigDecimal.ZERO, new BigDecimal("3000.00"),
                    "Direct Deposit", LedgerEntryStatus.POSTED, t0.plusDays(2));

            saveEntry(wallet.getId(), 4L, "TX-5", LedgerEntryType.DEBIT,
                    new BigDecimal("500.00"), new BigDecimal("3000.00"), new BigDecimal("2500.00"),
                    "Transfer to User #2", LedgerEntryStatus.POSTED, t0.plusDays(16));
        }

        log.info("Historical ledger entries populated successfully.");
    }

    private void saveEntry(
            Long walletId,
            Long userId,
            String txRef,
            LedgerEntryType type,
            BigDecimal amount,
            BigDecimal before,
            BigDecimal after,
            String desc,
            LedgerEntryStatus status,
            LocalDateTime createdAt
    ) {
        LedgerEntry entry = new LedgerEntry(
                walletId,
                userId,
                txRef,
                type,
                amount,
                before,
                after,
                desc,
                status
        );
        entry.setCreatedAt(createdAt);
        ledgerEntryRepository.save(entry);
    }
}
