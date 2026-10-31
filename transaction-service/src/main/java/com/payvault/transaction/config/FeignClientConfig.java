package com.payvault.transaction.config;

import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfig {

    /**
     * Limited retries ONLY for safe, read-only operations (like UserClient).
     * 1 initial attempt + 1 retry (maxAttempts = 2), initial backoff 100ms, max 1000ms.
     * Note: Debit and Credit operations on WalletClient remain NEVER_RETRY to prevent duplicate charges.
     */
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(100, 1000, 2);
    }
}
