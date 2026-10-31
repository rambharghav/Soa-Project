package com.payvault.wallet.config;

import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfig {

    /**
     * Limited retries ONLY for safe, read-only operations (like UserClient).
     * 1 initial attempt + 1 retry (maxAttempts = 2), initial backoff 100ms, max 1000ms.
     */
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(100, 1000, 2);
    }
}
