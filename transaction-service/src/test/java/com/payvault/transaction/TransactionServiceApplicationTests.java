package com.payvault.transaction;

import com.payvault.transaction.client.UserClient;
import com.payvault.transaction.client.WalletClient;
import com.payvault.transaction.dto.TransferRequest;
import com.payvault.transaction.entity.Transaction;
import com.payvault.transaction.entity.TransactionStatus;
import com.payvault.transaction.repository.TransactionRepository;
import com.payvault.transaction.service.TransactionServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.payvault.transaction.exception.ServiceUnavailableException;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TransactionServiceApplicationTests {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private WalletClient walletClient;

    @Mock
    private CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Mock
    private CircuitBreaker circuitBreaker;

    private TransactionServiceImpl transactionService;

    @BeforeEach
    void setUp() {

        MockitoAnnotations.openMocks(this);

        transactionService = new TransactionServiceImpl(
                transactionRepository,
                userClient,
                walletClient,
                circuitBreakerFactory
        );
    }

    @Test
    void contextLoads() {

        assertNotNull(transactionService);
    }

    @Test
    void transferShouldRejectSameSenderAndReceiver() {

        TransferRequest request =
                new TransferRequest(
                        2L,
                        2L,
                        new BigDecimal("100.00")
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> transactionService.transfer(request)
                );

        assertEquals(
                "Sender and receiver cannot be the same user",
                exception.getMessage()
        );

        verifyNoInteractions(
                userClient,
                walletClient,
                transactionRepository
        );
    }

    @Test
    void transferShouldRejectZeroAmount() {

        TransferRequest request =
                new TransferRequest(
                        2L,
                        3L,
                        BigDecimal.ZERO
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> transactionService.transfer(request)
                );

        assertEquals(
                "Transfer amount must be greater than zero",
                exception.getMessage()
        );

        verifyNoInteractions(
                userClient,
                walletClient,
                transactionRepository
        );
    }

    @Test
    void transferShouldRejectNegativeAmount() {

        TransferRequest request =
                new TransferRequest(
                        2L,
                        3L,
                        new BigDecimal("-50.00")
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> transactionService.transfer(request)
                );

        assertEquals(
                "Transfer amount must be greater than zero",
                exception.getMessage()
        );

        verifyNoInteractions(
                userClient,
                walletClient,
                transactionRepository
        );
    }

    @Test
    void transferShouldCompleteSuccessfully() {

        TransferRequest request =
                new TransferRequest(
                        2L,
                        3L,
                        new BigDecimal("100.00")
                );

        when(circuitBreakerFactory.create("userService"))
                .thenReturn(circuitBreaker);

        when(circuitBreakerFactory.create("walletService"))
                .thenReturn(circuitBreaker);

        when(circuitBreaker.run(any(), any()))
                .thenAnswer(invocation -> {

                    java.util.function.Supplier<?> supplier =
                            invocation.getArgument(0);

                    return supplier.get();
                });

        when(userClient.getUserById(2L))
                .thenReturn(null);

        when(userClient.getUserById(3L))
                .thenReturn(null);

        Transaction savedTransaction =
                new Transaction(
                        2L,
                        3L,
                        new BigDecimal("100.00")
                );

        savedTransaction.setId(1L);

        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(savedTransaction);

        assertDoesNotThrow(
                () -> transactionService.transfer(request)
        );

        verify(walletClient)
                .debit(
                        2L,
                        new BigDecimal("100.00")
                );

        verify(walletClient)
                .credit(
                        3L,
                        new BigDecimal("100.00")
                );

        verify(transactionRepository, atLeast(2))
                .save(any(Transaction.class));
    }

    @Test
    void transferShouldFailWhenWalletServiceIsUnavailable() {

        CircuitBreaker userCircuitBreaker =
                mock(CircuitBreaker.class);

        CircuitBreaker walletCircuitBreaker =
                mock(CircuitBreaker.class);

        when(circuitBreakerFactory.create("userService"))
                .thenReturn(userCircuitBreaker);

        when(circuitBreakerFactory.create("walletService"))
                .thenReturn(walletCircuitBreaker);

        // User Service works normally
        when(userCircuitBreaker.run(any(), any()))
                .thenAnswer(invocation -> {

                    java.util.function.Supplier<?> supplier =
                            invocation.getArgument(0);

                    return supplier.get();
                });

        // Wallet Service fails
        when(walletCircuitBreaker.run(any(), any()))
                .thenAnswer(invocation -> {

                    java.util.function.Function<Throwable, ?> fallback =
                            invocation.getArgument(1);

                    return fallback.apply(
                            new RuntimeException(
                                    "Wallet Service unavailable"
                            )
                    );
                });

        when(userClient.getUserById(anyLong()))
                .thenReturn(null);

        Transaction savedTransaction =
                new Transaction(
                        2L,
                        3L,
                        new BigDecimal("50.00")
                );

        savedTransaction.setId(10L);

        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(savedTransaction);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> transactionService.transfer(
                                new TransferRequest(
                                        2L,
                                        3L,
                                        new BigDecimal("50.00")
                                )
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains("Transfer failed")
        );

        verify(walletClient, never())
                .debit(
                        anyLong(),
                        any(BigDecimal.class)
                );

        verify(walletClient, never())
                .credit(
                        anyLong(),
                        any(BigDecimal.class)
                );

        verify(transactionRepository, atLeast(2))
                .save(any(Transaction.class));
    }

    @Test
    void transferShouldCompensateWhenReceiverCreditFails() {

        CircuitBreaker userCircuitBreaker =
                mock(CircuitBreaker.class);

        CircuitBreaker walletCircuitBreaker =
                mock(CircuitBreaker.class);

        when(circuitBreakerFactory.create("userService"))
                .thenReturn(userCircuitBreaker);

        when(circuitBreakerFactory.create("walletService"))
                .thenReturn(walletCircuitBreaker);

        // User Service works normally
        when(userCircuitBreaker.run(any(), any()))
                .thenAnswer(invocation -> {

                    java.util.function.Supplier<?> supplier =
                            invocation.getArgument(0);

                    return supplier.get();
                });

        /*
         * Wallet Service calls execute normally.
         * Receiver credit will fail below.
         */
        when(walletCircuitBreaker.run(any(), any()))
                .thenAnswer(invocation -> {

                    java.util.function.Supplier<?> supplier =
                            invocation.getArgument(0);

                    return supplier.get();
                });

        when(userClient.getUserById(anyLong()))
                .thenReturn(null);

        Transaction savedTransaction =
                new Transaction(
                        2L,
                        3L,
                        new BigDecimal("100.00")
                );

        savedTransaction.setId(20L);

        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(savedTransaction);

        /*
         * Sender debit succeeds.
         */
        when(walletClient.debit(
                2L,
                new BigDecimal("100.00")
        )).thenReturn(null);

        /*
         * Receiver credit fails.
         */
        when(walletClient.credit(
                3L,
                new BigDecimal("100.00")
        )).thenThrow(
                new IllegalArgumentException(
                        "Receiver credit failed"
                )
        );

        /*
         * Compensation credit to sender succeeds.
         */
        when(walletClient.credit(
                2L,
                new BigDecimal("100.00")
        )).thenReturn(null);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> transactionService.transfer(
                                new TransferRequest(
                                        2L,
                                        3L,
                                        new BigDecimal("100.00")
                                )
                        )
                );

        assertEquals(
        		"Transfer failed: Transfer failed while crediting receiver",
                exception.getMessage()
        );

        /*
         * Sender was debited once.
         */
        verify(walletClient, times(1))
                .debit(
                        2L,
                        new BigDecimal("100.00")
                );

        /*
         * Receiver credit was attempted once.
         */
        verify(walletClient, times(1))
                .credit(
                        3L,
                        new BigDecimal("100.00")
                );

        /*
         * Compensation credited sender back.
         */
        verify(walletClient, times(1))
                .credit(
                        2L,
                        new BigDecimal("100.00")
                );

        /*
         * Transaction saved initially
         * and again as FAILED.
         */
        verify(transactionRepository, atLeast(2))
                .save(any(Transaction.class));

        assertEquals(
                TransactionStatus.FAILED,
                savedTransaction.getStatus()
        );
    }

    @Test
    void getTransactionByIdShouldReturnTransaction() {

        Transaction transaction =
                new Transaction(
                        2L,
                        3L,
                        new BigDecimal("100.00")
                );

        transaction.setId(5L);

        transaction.setStatus(
                TransactionStatus.COMPLETED
        );

        when(transactionRepository.findById(5L))
                .thenReturn(
                        java.util.Optional.of(transaction)
                );

        var response =
                transactionService.getTransactionById(5L);

        assertNotNull(response);

        assertEquals(
                5L,
                response.getId()
        );

        assertEquals(
                2L,
                response.getSenderUserId()
        );

        assertEquals(
                3L,
                response.getReceiverUserId()
        );

        assertEquals(
                new BigDecimal("100.00"),
                response.getAmount()
        );

        assertEquals(
                TransactionStatus.COMPLETED,
                response.getStatus()
        );
    }

    @Test
    void getTransactionByIdShouldThrowWhenNotFound() {

        when(transactionRepository.findById(999L))
                .thenReturn(
                        java.util.Optional.empty()
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> transactionService.getTransactionById(999L)
                );

        assertEquals(
                "Transaction not found with id: 999",
                exception.getMessage()
        );
    }

    @Test
    void transferShouldThrowServiceUnavailableWhenUserServiceIsDown() {

        TransferRequest request =
                new TransferRequest(
                        2L,
                        3L,
                        new BigDecimal("100.00")
                );

        CircuitBreaker userCircuitBreaker = mock(CircuitBreaker.class);
        when(circuitBreakerFactory.create("userService"))
                .thenReturn(userCircuitBreaker);

        when(userCircuitBreaker.run(any(), any()))
                .thenAnswer(invocation -> {
                    java.util.function.Function<Throwable, ?> fallback = invocation.getArgument(1);
                    return fallback.apply(new RuntimeException("Connection refused to User Service"));
                });

        ServiceUnavailableException exception =
                assertThrows(
                        ServiceUnavailableException.class,
                        () -> transactionService.transfer(request)
                );

        assertTrue(exception.getMessage().contains("User Service is temporarily unavailable"));
        verifyNoInteractions(walletClient);
    }

    @Test
    void transferShouldHandleFeignBadRequestAsInsufficientBalance() {

        TransferRequest request =
                new TransferRequest(
                        2L,
                        3L,
                        new BigDecimal("500.00")
                );

        CircuitBreaker userCircuitBreaker = mock(CircuitBreaker.class);
        CircuitBreaker walletCircuitBreaker = mock(CircuitBreaker.class);

        when(circuitBreakerFactory.create("userService"))
                .thenReturn(userCircuitBreaker);
        when(circuitBreakerFactory.create("walletService"))
                .thenReturn(walletCircuitBreaker);

        // User service succeeds
        when(userCircuitBreaker.run(any(), any()))
                .thenAnswer(invocation -> {
                    java.util.function.Supplier<?> supplier = invocation.getArgument(0);
                    return supplier.get();
                });

        when(userClient.getUserById(anyLong())).thenReturn(null);

        Transaction savedTransaction = new Transaction(2L, 3L, new BigDecimal("500.00"));
        savedTransaction.setId(100L);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        // Wallet service debit triggers Feign BadRequest (insufficient balance)
        feign.Request feignRequest = feign.Request.create(
                feign.Request.HttpMethod.POST,
                "/api/wallets/user/2/debit",
                java.util.Collections.emptyMap(),
                null,
                java.nio.charset.StandardCharsets.UTF_8,
                null
        );
        feign.FeignException.BadRequest badRequest =
                new feign.FeignException.BadRequest("Insufficient wallet balance", feignRequest, null, null);

        when(walletCircuitBreaker.run(any(), any()))
                .thenAnswer(invocation -> {
                    java.util.function.Function<Throwable, ?> fallback = invocation.getArgument(1);
                    return fallback.apply(badRequest);
                });

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> transactionService.transfer(request)
                );

        assertTrue(exception.getMessage().contains("Insufficient wallet balance"));
        assertEquals(TransactionStatus.FAILED, savedTransaction.getStatus());
    }

    @Test
    void getUserTransactionsShouldReturnSentAndReceivedTransactions() {

        Transaction t1 = new Transaction(2L, 3L, new BigDecimal("100.00"));
        t1.setId(1L);
        Transaction t2 = new Transaction(4L, 2L, new BigDecimal("50.00"));
        t2.setId(2L);

        when(transactionRepository.findBySenderUserId(2L)).thenReturn(List.of(t1));
        when(transactionRepository.findByReceiverUserId(2L)).thenReturn(List.of(t2));

        var transactions = transactionService.getUserTransactions(2L);

        assertEquals(2, transactions.size());
        verify(transactionRepository).findBySenderUserId(2L);
        verify(transactionRepository).findByReceiverUserId(2L);
    }

    @Test
    void getAllTransactionsShouldReturnTransactions() {

        Transaction t1 = new Transaction(2L, 3L, new BigDecimal("100.00"));
        t1.setId(1L);

        when(transactionRepository.findAll()).thenReturn(List.of(t1));

        var transactions = transactionService.getAllTransactions();

        assertEquals(1, transactions.size());
        verify(transactionRepository).findAll();
    }
}