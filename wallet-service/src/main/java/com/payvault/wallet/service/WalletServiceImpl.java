package com.payvault.wallet.service;

import com.payvault.wallet.client.UserClient;
import com.payvault.wallet.dto.LedgerEntryResponse;
import com.payvault.wallet.dto.UserResponse;
import com.payvault.wallet.dto.WalletRequest;
import com.payvault.wallet.dto.WalletResponse;
import com.payvault.wallet.entity.LedgerEntry;
import com.payvault.wallet.entity.LedgerEntryStatus;
import com.payvault.wallet.entity.LedgerEntryType;
import com.payvault.wallet.entity.Wallet;
import com.payvault.wallet.exception.InsufficientBalanceException;
import com.payvault.wallet.exception.ServiceUnavailableException;
import com.payvault.wallet.exception.WalletNotFoundException;
import com.payvault.wallet.repository.LedgerEntryRepository;
import com.payvault.wallet.repository.WalletRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class WalletServiceImpl implements WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletServiceImpl.class);

    private final WalletRepository walletRepository;
    private final UserClient userClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    public WalletServiceImpl(
            WalletRepository walletRepository,
            UserClient userClient,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory,
            LedgerEntryRepository ledgerEntryRepository) {

        this.walletRepository = walletRepository;
        this.userClient = userClient;
        this.circuitBreakerFactory = circuitBreakerFactory;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    public WalletServiceImpl(
            WalletRepository walletRepository,
            UserClient userClient,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {

        this(walletRepository, userClient, circuitBreakerFactory, null);
    }

    public WalletServiceImpl(
            WalletRepository walletRepository,
            UserClient userClient) {

        this(walletRepository, userClient, null, null);
    }

    @Override
    @Transactional
    public WalletResponse createWallet(WalletRequest request) {

        validateUserExists(request.getUserId());

        if (walletRepository.existsByUserId(request.getUserId())) {
            throw new IllegalArgumentException(
                    "Wallet already exists for user id: "
                            + request.getUserId()
            );
        }

        Wallet wallet = new Wallet(request.getUserId());
        Wallet savedWallet = walletRepository.save(wallet);

        if (ledgerEntryRepository != null) {
            LedgerEntry initialEntry = new LedgerEntry(
                    savedWallet.getId(),
                    savedWallet.getUserId(),
                    "INIT-" + savedWallet.getId(),
                    LedgerEntryType.CREDIT,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    "Wallet account opened",
                    LedgerEntryStatus.POSTED
            );
            ledgerEntryRepository.save(initialEntry);
        }

        return mapToResponse(savedWallet);
    }

    @Override
    public WalletResponse getWalletById(Long id) {

        Wallet wallet = walletRepository.findById(id)
                .orElseThrow(() ->
                        new WalletNotFoundException(
                                "Wallet not found with id: " + id
                        )
                );

        return mapToResponse(wallet);
    }

    @Override
    public WalletResponse getWalletByUserId(Long userId) {

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new WalletNotFoundException(
                                "Wallet not found for user id: " + userId
                        )
                );

        return mapToResponse(wallet);
    }

    @Override
    public List<WalletResponse> getAllWallets() {

        return walletRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public WalletResponse credit(Long userId, BigDecimal amount) {
        return credit(userId, amount, null, null);
    }

    @Override
    @Transactional
    public WalletResponse credit(
            Long userId,
            BigDecimal amount,
            String transactionRef,
            String description) {

        validateAmount(amount);

        // Idempotency: Prevent duplicate credit if request was already processed
        if (transactionRef != null && !transactionRef.isBlank() && ledgerEntryRepository != null) {
            Optional<LedgerEntry> existing = ledgerEntryRepository
                    .findByUserIdAndTransactionRefAndEntryType(userId, transactionRef, LedgerEntryType.CREDIT);
            if (existing.isPresent()) {
                log.info("Idempotent retry detected for credit (userId={}, ref={}). Returning existing wallet balance.",
                        userId, transactionRef);
                Wallet existingWallet = walletRepository.findByUserId(userId)
                        .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user id: " + userId));
                return mapToResponse(existingWallet);
            }
        }

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new WalletNotFoundException(
                                "Wallet not found for user id: " + userId
                        )
                );

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);

        wallet.setBalance(balanceAfter);
        Wallet updatedWallet = walletRepository.save(wallet);

        if (ledgerEntryRepository != null) {
            String effectiveRef = (transactionRef != null && !transactionRef.isBlank())
                    ? transactionRef
                    : ("DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            String effectiveDesc = (description != null && !description.isBlank())
                    ? description
                    : "Wallet Credit";

            LedgerEntry entry = new LedgerEntry(
                    updatedWallet.getId(),
                    userId,
                    effectiveRef,
                    LedgerEntryType.CREDIT,
                    amount,
                    balanceBefore,
                    balanceAfter,
                    effectiveDesc,
                    LedgerEntryStatus.POSTED
            );
            ledgerEntryRepository.save(entry);
        }

        return mapToResponse(updatedWallet);
    }

    @Override
    @Transactional
    public WalletResponse debit(Long userId, BigDecimal amount) {
        return debit(userId, amount, null, null);
    }

    @Override
    @Transactional
    public WalletResponse debit(
            Long userId,
            BigDecimal amount,
            String transactionRef,
            String description) {

        validateAmount(amount);

        // Idempotency: Prevent duplicate debit if request was already processed
        if (transactionRef != null && !transactionRef.isBlank() && ledgerEntryRepository != null) {
            Optional<LedgerEntry> existing = ledgerEntryRepository
                    .findByUserIdAndTransactionRefAndEntryType(userId, transactionRef, LedgerEntryType.DEBIT);
            if (existing.isPresent()) {
                log.info("Idempotent retry detected for debit (userId={}, ref={}). Returning existing wallet balance.",
                        userId, transactionRef);
                Wallet existingWallet = walletRepository.findByUserId(userId)
                        .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user id: " + userId));
                return mapToResponse(existingWallet);
            }
        }

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new WalletNotFoundException(
                                "Wallet not found for user id: " + userId
                        )
                );

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient wallet balance"
            );
        }

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.subtract(amount);

        wallet.setBalance(balanceAfter);
        Wallet updatedWallet = walletRepository.save(wallet);

        if (ledgerEntryRepository != null) {
            String effectiveRef = (transactionRef != null && !transactionRef.isBlank())
                    ? transactionRef
                    : ("TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            String effectiveDesc = (description != null && !description.isBlank())
                    ? description
                    : "Wallet Debit";

            LedgerEntry entry = new LedgerEntry(
                    updatedWallet.getId(),
                    userId,
                    effectiveRef,
                    LedgerEntryType.DEBIT,
                    amount,
                    balanceBefore,
                    balanceAfter,
                    effectiveDesc,
                    LedgerEntryStatus.POSTED
            );
            ledgerEntryRepository.save(entry);
        }

        return mapToResponse(updatedWallet);
    }

    @Override
    @Transactional
    public void deleteWallet(Long id) {

        Wallet wallet = walletRepository.findById(id)
                .orElseThrow(() ->
                        new WalletNotFoundException(
                                "Wallet not found with id: " + id
                        )
                );

        if (ledgerEntryRepository != null) {
            List<LedgerEntry> entries = ledgerEntryRepository.findByWalletIdOrderByCreatedAtDesc(id);
            if (!entries.isEmpty()) {
                throw new IllegalStateException(
                        "Cannot delete wallet with recorded ledger entries. Financial ledger is immutable and auditable."
                );
            }
        }

        walletRepository.delete(wallet);
    }

    @Override
    public List<LedgerEntryResponse> getLedgerEntriesByUserId(Long userId) {
        return getLedgerEntriesByUserId(userId, null, null);
    }

    @Override
    public List<LedgerEntryResponse> getLedgerEntriesByUserId(Long userId, String authUser, String authRole) {

        // Enforce user-level authorization if auth identity is passed from Gateway
        if (authUser != null && !authUser.isBlank() && !"ADMIN".equalsIgnoreCase(authRole)) {
            validateUserOwnership(userId, authUser);
        }

        // Verify wallet exists
        walletRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new WalletNotFoundException(
                                "Wallet not found for user id: " + userId
                        )
                );

        if (ledgerEntryRepository == null) {
            return new ArrayList<>();
        }

        return ledgerEntryRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToLedgerResponse)
                .toList();
    }

    private void validateUserOwnership(Long userId, String authUser) {
        UserResponse user = fetchUserWithCircuitBreaker(userId);
        if (user == null) {
            throw new SecurityException("Access denied: Unable to verify user identity for user id: " + userId);
        }

        String username = authUser.trim().toLowerCase();

        // 1. Direct ID match
        boolean idMatch = String.valueOf(user.getId()).equals(username);

        // 2. Name matching: exact, compact, or individual words
        boolean nameMatch = false;
        if (user.getName() != null && !user.getName().isBlank()) {
            String nameLower = user.getName().trim().toLowerCase();
            String nameCompact = nameLower.replace(" ", "");

            if (nameLower.equals(username)
                    || nameCompact.equals(username)
                    || nameCompact.contains(username)
                    || username.contains(nameCompact)) {
                nameMatch = true;
            } else {
                for (String part : nameLower.split("[\\s_\\-\\.]+")) {
                    if (part.equals(username)) {
                        nameMatch = true;
                        break;
                    }
                }
            }
        }

        // 3. Email matching: exact, prefix before @, or prefix matching username
        boolean emailMatch = false;
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            String emailLower = user.getEmail().trim().toLowerCase();
            String prefix = emailLower.contains("@")
                    ? emailLower.substring(0, emailLower.indexOf("@"))
                    : emailLower;

            if (emailLower.equals(username)
                    || prefix.equals(username)
                    || prefix.contains(username)
                    || username.contains(prefix)) {
                emailMatch = true;
            }
        }

        if (!idMatch && !nameMatch && !emailMatch) {
            throw new SecurityException(
                    "Access denied: You are not authorized to view the financial ledger for user id " + userId
            );
        }
    }

    private UserResponse fetchUserWithCircuitBreaker(Long userId) {
        if (circuitBreakerFactory != null) {
            CircuitBreaker userServiceBreaker = circuitBreakerFactory.create("userService");
            return userServiceBreaker.run(
                    () -> {
                        try {
                            return userClient.getUserById(userId);
                        } catch (feign.FeignException.NotFound ex) {
                            return null;
                        }
                    },
                    throwable -> {
                        throw new ServiceUnavailableException(
                                "User Service is temporarily unavailable. Cannot verify identity."
                        );
                    }
            );
        } else {
            try {
                return userClient.getUserById(userId);
            } catch (Exception ex) {
                if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("not found")) {
                    return null;
                }
                throw new ServiceUnavailableException(
                        "User Service is temporarily unavailable. Cannot verify identity."
                );
            }
        }
    }

    private void validateAmount(BigDecimal amount) {

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Amount must be greater than zero"
            );
        }
    }

    private WalletResponse mapToResponse(Wallet wallet) {

        return new WalletResponse(
                wallet.getId(),
                wallet.getUserId(),
                wallet.getBalance(),
                wallet.getCurrency(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }

    private LedgerEntryResponse mapToLedgerResponse(LedgerEntry entry) {

        return new LedgerEntryResponse(
                entry.getId(),
                entry.getWalletId(),
                entry.getUserId(),
                entry.getTransactionRef(),
                entry.getEntryType(),
                entry.getAmount(),
                entry.getBalanceBefore(),
                entry.getBalanceAfter(),
                entry.getDescription(),
                entry.getStatus(),
                entry.getCreatedAt()
        );
    }

    private void validateUserExists(Long userId) {
        if (circuitBreakerFactory != null) {
            CircuitBreaker userServiceBreaker = circuitBreakerFactory.create("userService");
            userServiceBreaker.run(
                    () -> {
                        try {
                            return userClient.getUserById(userId);
                        } catch (feign.FeignException.NotFound ex) {
                            throw new IllegalArgumentException(
                                     "User not found with id: " + userId
                            );
                        }
                    },
                    throwable -> {
                        if (throwable instanceof IllegalArgumentException) {
                            throw (IllegalArgumentException) throwable;
                        }
                        if (throwable.getMessage() != null && throwable.getMessage().toLowerCase().contains("user not found")) {
                            throw new IllegalArgumentException(
                                    "User not found with id: " + userId
                            );
                        }
                        throw new ServiceUnavailableException(
                                "User Service is temporarily unavailable. Cannot verify user."
                        );
                    }
            );
        } else {
            try {
                userClient.getUserById(userId);
            } catch (Exception ex) {
                if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("user not found")) {
                    throw new IllegalArgumentException(
                            "User not found with id: " + userId
                    );
                }
                throw new ServiceUnavailableException(
                        "User Service is temporarily unavailable. Cannot verify user."
                );
            }
        }
    }
}