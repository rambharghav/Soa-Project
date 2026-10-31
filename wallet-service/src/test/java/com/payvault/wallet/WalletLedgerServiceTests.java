package com.payvault.wallet;

import com.payvault.wallet.client.UserClient;
import com.payvault.wallet.dto.LedgerEntryResponse;
import com.payvault.wallet.dto.UserResponse;
import com.payvault.wallet.dto.WalletResponse;
import com.payvault.wallet.entity.LedgerEntry;
import com.payvault.wallet.entity.LedgerEntryStatus;
import com.payvault.wallet.entity.LedgerEntryType;
import com.payvault.wallet.entity.Wallet;
import com.payvault.wallet.repository.LedgerEntryRepository;
import com.payvault.wallet.repository.WalletRepository;
import com.payvault.wallet.service.WalletServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WalletLedgerServiceTests {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Mock
    private CircuitBreaker circuitBreaker;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    private WalletServiceImpl walletService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(circuitBreakerFactory.create(anyString())).thenReturn(circuitBreaker);
        when(circuitBreaker.run(any(), any())).thenAnswer(invocation -> {
            java.util.function.Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        });

        walletService = new WalletServiceImpl(
                walletRepository,
                userClient,
                circuitBreakerFactory,
                ledgerEntryRepository
        );
    }

    @Test
    @DisplayName("Credit should record a ledger entry with running balance")
    void testCreditRecordsLedgerEntryWithRunningBalance() {
        Wallet wallet = new Wallet(1L);
        wallet.setId(10L);
        wallet.setBalance(new BigDecimal("100.00"));

        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        WalletResponse res = walletService.credit(1L, new BigDecimal("50.00"), "TX-101", "Deposit");

        assertEquals(new BigDecimal("150.00"), res.getBalance());

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository, times(1)).save(captor.capture());

        LedgerEntry entry = captor.getValue();
        assertEquals(1L, entry.getUserId());
        assertEquals("TX-101", entry.getTransactionRef());
        assertEquals(LedgerEntryType.CREDIT, entry.getEntryType());
        assertEquals(new BigDecimal("50.00"), entry.getAmount());
        assertEquals(new BigDecimal("100.00"), entry.getBalanceBefore());
        assertEquals(new BigDecimal("150.00"), entry.getBalanceAfter());
        assertEquals("Deposit", entry.getDescription());
        assertEquals(LedgerEntryStatus.POSTED, entry.getStatus());
    }

    @Test
    @DisplayName("Debit should record a ledger entry with reduced running balance")
    void testDebitRecordsLedgerEntryWithRunningBalance() {
        Wallet wallet = new Wallet(1L);
        wallet.setId(10L);
        wallet.setBalance(new BigDecimal("200.00"));

        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        WalletResponse res = walletService.debit(1L, new BigDecimal("70.00"), "TX-102", "Transfer Out");

        assertEquals(new BigDecimal("130.00"), res.getBalance());

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository, times(1)).save(captor.capture());

        LedgerEntry entry = captor.getValue();
        assertEquals(1L, entry.getUserId());
        assertEquals("TX-102", entry.getTransactionRef());
        assertEquals(LedgerEntryType.DEBIT, entry.getEntryType());
        assertEquals(new BigDecimal("70.00"), entry.getAmount());
        assertEquals(new BigDecimal("200.00"), entry.getBalanceBefore());
        assertEquals(new BigDecimal("130.00"), entry.getBalanceAfter());
        assertEquals("Transfer Out", entry.getDescription());
        assertEquals(LedgerEntryStatus.POSTED, entry.getStatus());
    }

    @Test
    @DisplayName("Idempotency: duplicate debit retry does not mutate wallet balance or insert duplicate entry")
    void testDuplicateDebitRetryIsPrevented() {
        Wallet wallet = new Wallet(1L);
        wallet.setId(10L);
        wallet.setBalance(new BigDecimal("500.00"));

        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        // Mock that entry already exists for TX-DUPLICATE
        LedgerEntry existingEntry = new LedgerEntry(10L, 1L, "TX-DUPLICATE", LedgerEntryType.DEBIT,
                new BigDecimal("50.00"), new BigDecimal("550.00"), new BigDecimal("500.00"), "Transfer", LedgerEntryStatus.POSTED);
        when(ledgerEntryRepository.findByUserIdAndTransactionRefAndEntryType(1L, "TX-DUPLICATE", LedgerEntryType.DEBIT))
                .thenReturn(Optional.of(existingEntry));

        WalletResponse response = walletService.debit(1L, new BigDecimal("50.00"), "TX-DUPLICATE", "Transfer retry");

        // Balance remains unchanged (500.00)
        assertEquals(new BigDecimal("500.00"), response.getBalance());
        // save is NEVER called again on repository
        verify(ledgerEntryRepository, never()).save(any(LedgerEntry.class));
    }

    @Test
    @DisplayName("Auditability: Cannot delete a wallet that has posted ledger entries")
    void testCannotDeleteWalletWithRecordedLedgerEntries() {
        Wallet wallet = new Wallet(1L);
        wallet.setId(10L);
        when(walletRepository.findById(10L)).thenReturn(Optional.of(wallet));

        LedgerEntry entry = new LedgerEntry(10L, 1L, "TX-1", LedgerEntryType.CREDIT,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"), "Init", LedgerEntryStatus.POSTED);
        when(ledgerEntryRepository.findByWalletIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(entry));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> walletService.deleteWallet(10L));
        assertTrue(ex.getMessage().contains("Cannot delete wallet with recorded ledger entries"));
        verify(walletRepository, never()).delete(any());
    }

    @Test
    @DisplayName("User authorization: User can view their own ledger")
    void testUserCanViewOwnLedger() {
        Wallet wallet = new Wallet(1L);
        wallet.setId(10L);
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));

        UserResponse user = new UserResponse(1L, "kavi", "kavi@example.com", "9999999999", LocalDateTime.now());
        when(userClient.getUserById(1L)).thenReturn(user);

        LedgerEntry entry = new LedgerEntry(10L, 1L, "TX-1", LedgerEntryType.CREDIT,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"), "Deposit", LedgerEntryStatus.POSTED);
        when(ledgerEntryRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(entry));

        List<LedgerEntryResponse> list = walletService.getLedgerEntriesByUserId(1L, "kavi", "USER");
        assertEquals(1, list.size());
        assertEquals("TX-1", list.get(0).getTransactionRef());
        assertEquals(new BigDecimal("100.00"), list.get(0).getBalanceAfter());
    }

    @Test
    @DisplayName("User authorization: User CANNOT view another user's ledger")
    void testUserCannotViewOtherUserLedger() {
        UserResponse user = new UserResponse(2L, "bob", "bob@example.com", "8888888888", LocalDateTime.now());
        when(userClient.getUserById(2L)).thenReturn(user);

        // Authenticated user is "alice", but requesting ledger for user 2 ("bob")
        SecurityException ex = assertThrows(SecurityException.class, () ->
                walletService.getLedgerEntriesByUserId(2L, "alice", "USER")
        );
        assertTrue(ex.getMessage().contains("Access denied"));
        verify(ledgerEntryRepository, never()).findByUserIdOrderByCreatedAtDesc(anyLong());
    }

    @Test
    @DisplayName("User authorization: Admin CAN view any user's ledger")
    void testAdminCanViewAnyUserLedger() {
        Wallet wallet = new Wallet(2L);
        wallet.setId(20L);
        when(walletRepository.findByUserId(2L)).thenReturn(Optional.of(wallet));

        when(ledgerEntryRepository.findByUserIdOrderByCreatedAtDesc(2L)).thenReturn(List.of());

        List<LedgerEntryResponse> list = walletService.getLedgerEntriesByUserId(2L, "admin", "ADMIN");
        assertNotNull(list);
    }

    @Test
    @DisplayName("User authorization: sandilya username matches Kavi Sandilya profile")
    void testSandilyaUserCanViewKaviSandilyaLedger() {
        Wallet wallet = new Wallet(1L);
        wallet.setId(10L);
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));

        UserResponse user = new UserResponse(1L, "Kavi Sandilya", "sandilyakavi@gmail.com", "9876543210", LocalDateTime.now());
        when(userClient.getUserById(1L)).thenReturn(user);

        LedgerEntry entry = new LedgerEntry(10L, 1L, "TX-1", LedgerEntryType.CREDIT,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("500.00"), "Deposit", LedgerEntryStatus.POSTED);
        when(ledgerEntryRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(entry));

        List<LedgerEntryResponse> list = walletService.getLedgerEntriesByUserId(1L, "sandilya", "USER");
        assertEquals(1, list.size());
        assertEquals("TX-1", list.get(0).getTransactionRef());
    }

    @Test
    @DisplayName("User authorization: raviraj username matches Devkar RaviRaj profile")
    void testRavirajUserCanViewDevkarRavirajLedger() {
        Wallet wallet = new Wallet(2L);
        wallet.setId(20L);
        when(walletRepository.findByUserId(2L)).thenReturn(Optional.of(wallet));

        UserResponse user = new UserResponse(2L, "Devkar RaviRaj", "raviraj@gmail.com", "9123456789", LocalDateTime.now());
        when(userClient.getUserById(2L)).thenReturn(user);

        LedgerEntry entry = new LedgerEntry(20L, 2L, "TX-2", LedgerEntryType.CREDIT,
                new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), "Transfer", LedgerEntryStatus.POSTED);
        when(ledgerEntryRepository.findByUserIdOrderByCreatedAtDesc(2L)).thenReturn(List.of(entry));

        List<LedgerEntryResponse> list = walletService.getLedgerEntriesByUserId(2L, "raviraj", "USER");
        assertEquals(1, list.size());
        assertEquals("TX-2", list.get(0).getTransactionRef());
    }

    @Test
    @DisplayName("User authorization: raviraj CANNOT view sandilya's ledger")
    void testRavirajCannotViewSandilyaLedger() {
        UserResponse user = new UserResponse(1L, "Kavi Sandilya", "sandilyakavi@gmail.com", "9876543210", LocalDateTime.now());
        when(userClient.getUserById(1L)).thenReturn(user);

        SecurityException ex = assertThrows(SecurityException.class, () ->
                walletService.getLedgerEntriesByUserId(1L, "raviraj", "USER")
        );
        assertTrue(ex.getMessage().contains("Access denied: You are not authorized to view the financial ledger for user id 1"));
    }

    @Test
    @DisplayName("User authorization: sandilya CANNOT view raviraj's ledger")
    void testSandilyaCannotViewRavirajLedger() {
        UserResponse user = new UserResponse(2L, "Devkar RaviRaj", "raviraj@gmail.com", "9123456789", LocalDateTime.now());
        when(userClient.getUserById(2L)).thenReturn(user);

        SecurityException ex = assertThrows(SecurityException.class, () ->
                walletService.getLedgerEntriesByUserId(2L, "sandilya", "USER")
        );
        assertTrue(ex.getMessage().contains("Access denied: You are not authorized to view the financial ledger for user id 2"));
    }
}
