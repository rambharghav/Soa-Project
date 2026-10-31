package com.payvault.transaction.client;

import com.payvault.transaction.config.FeignClientConfig;
import com.payvault.transaction.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "USER-SERVICE", configuration = FeignClientConfig.class)
public interface UserClient {

    @GetMapping("/api/users/{id}")
    UserResponse getUserById(
            @PathVariable("id") Long id
    );
}