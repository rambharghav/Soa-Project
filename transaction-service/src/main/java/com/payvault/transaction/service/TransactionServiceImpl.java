package com.payvault.transaction.service;

import com.payvault.transaction.client.UserClient;
import com.payvault.transaction.client.WalletClient;
import com.payvault.transaction.dto.TransactionResponse;
import com.payvault.transaction.dto.TransferRequest;
import com.payvault.transaction.entity.Transaction;
import com.payvault.transaction.entity.TransactionStatus;
import com.payvault.transaction.exception.ServiceUnavailableException;
import com.payvault.transaction.repository.TransactionRepository;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserClient userClient;
    private final WalletClient walletClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public TransactionServiceImpl(
            TransactionRepository transactionRepository,
            UserClient userClient,
            WalletClient walletClient,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory
    ) {
        this.transactionRepository = transactionRepository;
        this.userClient = userClient;
        this.walletClient = walletClient;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    @Override
    public TransactionResponse transfer(TransferRequest request) {

        validateTransfer(request);

        // 1. Validate sender
        validateUser(
                request.getSenderUserId(),
                "Sender"
        );

        // 2. Validate receiver
        validateUser(
                request.getReceiverUserId(),
                "Receiver"
        );

        Transaction transaction = new Transaction(
                request.getSenderUserId(),
                request.getReceiverUserId(),
                request.getAmount()
        );

        Transaction savedTransaction =
                transactionRepository.save(transaction);

        boolean senderDebited = false;

        try {

            // 3. Debit sender wallet
            String txRef = "TX-" + savedTransaction.getId();
            try {
                com.payvault.transaction.context.LedgerContext.set(
                        txRef,
                        "Transfer to User #" + request.getReceiverUserId()
                );
                debitWallet(
                        request.getSenderUserId(),
                        request.getAmount()
                );
            } finally {
                com.payvault.transaction.context.LedgerContext.clear();
            }

            senderDebited = true;

            try {

                // 4. Credit receiver wallet
                try {
                    com.payvault.transaction.context.LedgerContext.set(
                            txRef,
                            "Transfer from User #" + request.getSenderUserId()
                    );
                    creditWallet(
                            request.getReceiverUserId(),
                            request.getAmount()
                    );
                } finally {
                    com.payvault.transaction.context.LedgerContext.clear();
                }

                // 5. Transfer successful
                savedTransaction.setStatus(
                        TransactionStatus.COMPLETED
                );

            } catch (Exception creditException) {

                /*
                 * Receiver credit failed.
                 * Sender was already debited.
                 * So compensate by crediting sender back.
                 */

                if (senderDebited) {

                    try {
                        try {
                            com.payvault.transaction.context.LedgerContext.set(
                                    "REV-" + savedTransaction.getId(),
                                    "Reversal: Refund for failed transfer #"
                                            + savedTransaction.getId()
                                            + " to User #"
                                            + request.getReceiverUserId()
                            );
                            creditWallet(
                                    request.getSenderUserId(),
                                    request.getAmount()
                            );
                        } finally {
                            com.payvault.transaction.context.LedgerContext.clear();
                        }

                    } catch (Exception compensationException) {

                        // Compensation failed.
                        // Transaction remains FAILED.
                    }
                }

                savedTransaction.setStatus(
                        TransactionStatus.FAILED
                );

                transactionRepository.save(savedTransaction);

                if (creditException instanceof ServiceUnavailableException) {
                    throw new ServiceUnavailableException(
                            "Transfer failed while crediting receiver"
                    );
                }

                throw new IllegalArgumentException(
                        "Transfer failed while crediting receiver"
                );
            }

        } catch (Exception debitException) {

            savedTransaction.setStatus(
                    TransactionStatus.FAILED
            );

            transactionRepository.save(savedTransaction);

            if (debitException instanceof ServiceUnavailableException) {
                throw new ServiceUnavailableException(
                        "Transfer failed: "
                                + getErrorMessage(debitException)
                );
            }

            throw new IllegalArgumentException(
                    "Transfer failed: "
                            + getErrorMessage(debitException)
            );
        }

        Transaction completedTransaction =
                transactionRepository.save(savedTransaction);

        return mapToResponse(completedTransaction);
    }

    /*
     * -----------------------------------------
     * USER SERVICE CIRCUIT BREAKER
     * -----------------------------------------
     */

    private void validateUser(
            Long userId,
            String userType
    ) {

        CircuitBreaker userServiceBreaker =
                circuitBreakerFactory.create("userService");

        try {

            userServiceBreaker.run(
                    () -> userClient.getUserById(userId),

                    throwable -> {

                        throw new ServiceUnavailableException(
                                "User Service is temporarily unavailable"
                        );
                    }
            );

        } catch (ServiceUnavailableException ex) {

            throw ex;

        } catch (IllegalArgumentException ex) {

            throw ex;

        } catch (Exception ex) {

            throw new IllegalArgumentException(
                    userType
                            + " user not found with id: "
                            + userId
            );
        }
    }

    /*
     * -----------------------------------------
     * WALLET SERVICE CIRCUIT BREAKER
     * -----------------------------------------
     */

    private void debitWallet(
            Long userId,
            BigDecimal amount
    ) {

        CircuitBreaker walletServiceBreaker =
                circuitBreakerFactory.create("walletService");

        try {

            walletServiceBreaker.run(
                    () -> {

                        walletClient.debit(
                                userId,
                                amount
                        );

                        return null;
                    },

                    throwable -> {

                        if (throwable instanceof feign.FeignException.BadRequest) {

                            throw new IllegalArgumentException(
                                    "Insufficient wallet balance"
                            );
                        }

                        throw new ServiceUnavailableException(
                                "Wallet Service is temporarily unavailable"
                        );
                    }
            );

        } catch (IllegalArgumentException ex) {

            throw ex;

        } catch (Exception ex) {

            throw new ServiceUnavailableException(
                    "Wallet Service is temporarily unavailable"
            );
        }
    }

    private void creditWallet(
            Long userId,
            BigDecimal amount
    ) {

        CircuitBreaker walletServiceBreaker =
                circuitBreakerFactory.create("walletService");

        walletServiceBreaker.run(
                () -> {

                    walletClient.credit(
                            userId,
                            amount
                    );

                    return null;
                },

                throwable -> {

                    throw new ServiceUnavailableException(
                            "Wallet Service is temporarily unavailable"
                    );
                }
        );
    }

    /*
     * -----------------------------------------
     * GET TRANSACTION
     * -----------------------------------------
     */

    @Override
    public TransactionResponse getTransactionById(
            Long id
    ) {

        Transaction transaction =
                transactionRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Transaction not found with id: "
                                                + id
                                )
                        );

        return mapToResponse(transaction);
    }

    /*
     * -----------------------------------------
     * GET ALL TRANSACTIONS
     * -----------------------------------------
     */

    @Override
    public List<TransactionResponse> getAllTransactions() {

        List<Transaction> transactions =
                transactionRepository.findAll();

        List<TransactionResponse> responses =
                new ArrayList<>();

        for (Transaction transaction : transactions) {

            responses.add(
                    mapToResponse(transaction)
            );
        }

        return responses;
    }

    /*
     * -----------------------------------------
     * GET USER TRANSACTIONS
     * -----------------------------------------
     */

    @Override
    public List<TransactionResponse> getUserTransactions(
            Long userId
    ) {

        List<Transaction> sent =
                transactionRepository.findBySenderUserId(userId);

        List<Transaction> received =
                transactionRepository.findByReceiverUserId(userId);

        List<Transaction> allTransactions =
                new ArrayList<>();

        allTransactions.addAll(sent);
        allTransactions.addAll(received);

        List<TransactionResponse> responses =
                new ArrayList<>();

        for (Transaction transaction : allTransactions) {

            responses.add(
                    mapToResponse(transaction)
            );
        }

        return responses;
    }

    /*
     * -----------------------------------------
     * VALIDATION
     * -----------------------------------------
     */

    private void validateTransfer(
            TransferRequest request
    ) {

        if (request.getSenderUserId()
                .equals(request.getReceiverUserId())) {

            throw new IllegalArgumentException(
                    "Sender and receiver cannot be the same user"
            );
        }

        if (request.getAmount() == null ||
                request.getAmount()
                        .compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Transfer amount must be greater than zero"
            );
        }
    }

    /*
     * -----------------------------------------
     * ERROR MESSAGE
     * -----------------------------------------
     */

    private String getErrorMessage(
            Exception ex
    ) {

        if (ex.getMessage() != null &&
                !ex.getMessage().isBlank()) {

            return ex.getMessage();
        }

        return "Unable to process transfer";
    }

    /*
     * -----------------------------------------
     * ENTITY → RESPONSE
     * -----------------------------------------
     */

    private TransactionResponse mapToResponse(
            Transaction transaction
    ) {

        return new TransactionResponse(
                transaction.getId(),
                transaction.getSenderUserId(),
                transaction.getReceiverUserId(),
                transaction.getAmount(),
                transaction.getStatus(),
                transaction.getCreatedAt()
        );
    }
}