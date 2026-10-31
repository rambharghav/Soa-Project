package com.payvault.gateway.filter;

import com.payvault.gateway.util.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    public static final String HEADER_USER = "X-Auth-User";
    public static final String HEADER_ROLE = "X-Auth-Role";

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {

        String path = exchange.getRequest()
                .getURI()
                .getPath();

        // Auth and fallback endpoints don't require JWT, but strip any client-spoofed identity headers
        if (path.startsWith("/api/auth/") || path.startsWith("/fallback/") || path.startsWith("/actuator/")) {
            ServerWebExchange sanitizedExchange = exchange.mutate()
                    .request(r -> r.headers(headers -> {
                        headers.remove(HEADER_USER);
                        headers.remove(HEADER_ROLE);
                    }))
                    .build();
            return chain.filter(sanitizedExchange);
        }

        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null ||
                !authHeader.startsWith("Bearer ")) {

            exchange.getResponse()
                    .setStatusCode(HttpStatus.UNAUTHORIZED);

            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.isTokenValid(token)) {

            exchange.getResponse()
                    .setStatusCode(HttpStatus.UNAUTHORIZED);

            return exchange.getResponse().setComplete();
        }

        // Token is valid. Strip any client-supplied identity headers and propagate verified claims.
        String username = jwtUtil.extractUsername(token);
        String role = jwtUtil.extractRole(token);

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(r -> r.headers(headers -> {
                    headers.remove(HEADER_USER);
                    headers.remove(HEADER_ROLE);
                    if (username != null && !username.isBlank()) {
                        headers.set(HEADER_USER, username);
                    }
                    if (role != null && !role.isBlank()) {
                        headers.set(HEADER_ROLE, role);
                    }
                }))
                .build();

        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() {
        return -1;
    }
}