package com.payvault.transaction.client;

import com.payvault.transaction.dto.WalletResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "WALLET-SERVICE")
public interface WalletClient {

    @PostMapping("/api/wallets/user/{userId}/credit")
    WalletResponse credit(
            @PathVariable("userId") Long userId,
            @RequestParam("amount") BigDecimal amount
    );

    @PostMapping("/api/wallets/user/{userId}/debit")
    WalletResponse debit(
            @PathVariable("userId") Long userId,
            @RequestParam("amount") BigDecimal amount
    );
}