package com.payvault.gateway;

import com.payvault.gateway.filter.JwtAuthenticationFilter;
import com.payvault.gateway.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private GatewayFilterChain chain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new JwtAuthenticationFilter(jwtUtil);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void filterShouldAllowAuthEndpointsWithoutToken() {
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/api/auth/login")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        verify(chain, times(1)).filter(any());
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void filterShouldAllowFallbackEndpointsWithoutToken() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/fallback/wallets")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        verify(chain, times(1)).filter(any());
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void filterShouldRejectMissingAuthorizationHeader() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/wallets/1")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void filterShouldRejectMalformedAuthorizationHeader() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/wallets/1")
                .header(HttpHeaders.AUTHORIZATION, "Basic invalid-format")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void filterShouldRejectInvalidJwtToken() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/wallets/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token.value")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.isTokenValid("invalid.token.value")).thenReturn(false);

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void filterShouldProceedWithValidToken() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/wallets/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid.token.value")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.isTokenValid("valid.token.value")).thenReturn(true);
        when(jwtUtil.extractUsername("valid.token.value")).thenReturn("testuser");
        when(jwtUtil.extractRole("valid.token.value")).thenReturn("USER");

        filter.filter(exchange, chain).block();

        verify(chain, times(1)).filter(any());
    }

    @Test
    void filterShouldPropagateVerifiedClaimsDownstream() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/transactions/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token123")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.isTokenValid("token123")).thenReturn(true);
        when(jwtUtil.extractUsername("token123")).thenReturn("alice");
        when(jwtUtil.extractRole("token123")).thenReturn("USER");

        org.mockito.ArgumentCaptor<ServerWebExchange> captor =
                org.mockito.ArgumentCaptor.forClass(ServerWebExchange.class);

        filter.filter(exchange, chain).block();

        verify(chain).filter(captor.capture());
        ServerWebExchange captured = captor.getValue();
        assertEquals("alice", captured.getRequest().getHeaders().getFirst("X-Auth-User"));
        assertEquals("USER", captured.getRequest().getHeaders().getFirst("X-Auth-Role"));
    }

    @Test
    void filterShouldStripSpoofedHeadersOnAuthenticatedRequest() {
        // Attacker attempts to spoof X-Auth-User as admin
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/wallets/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token123")
                .header("X-Auth-User", "admin")
                .header("X-Auth-Role", "ADMIN")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.isTokenValid("token123")).thenReturn(true);
        when(jwtUtil.extractUsername("token123")).thenReturn("normaluser");
        when(jwtUtil.extractRole("token123")).thenReturn("USER");

        org.mockito.ArgumentCaptor<ServerWebExchange> captor =
                org.mockito.ArgumentCaptor.forClass(ServerWebExchange.class);

        filter.filter(exchange, chain).block();

        verify(chain).filter(captor.capture());
        ServerWebExchange captured = captor.getValue();
        // The spoofed admin header MUST be overwritten by the verified identity
        assertEquals("normaluser", captured.getRequest().getHeaders().getFirst("X-Auth-User"));
        assertEquals("USER", captured.getRequest().getHeaders().getFirst("X-Auth-Role"));
    }

    @Test
    void filterShouldStripSpoofedHeadersOnPublicAuthEndpoints() {
        // Attacker attempts to spoof X-Auth-User on public endpoint
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/api/auth/login")
                .header("X-Auth-User", "admin")
                .header("X-Auth-Role", "ADMIN")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        org.mockito.ArgumentCaptor<ServerWebExchange> captor =
                org.mockito.ArgumentCaptor.forClass(ServerWebExchange.class);

        filter.filter(exchange, chain).block();

        verify(chain).filter(captor.capture());
        ServerWebExchange captured = captor.getValue();
        // Spoofed headers must be stripped
        org.junit.jupiter.api.Assertions.assertNull(captured.getRequest().getHeaders().getFirst("X-Auth-User"));
        org.junit.jupiter.api.Assertions.assertNull(captured.getRequest().getHeaders().getFirst("X-Auth-Role"));
    }

    @Test
    void filterShouldStripSpoofedHeadersOnFallbackEndpoints() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/fallback/users")
                .header("X-Auth-User", "admin")
                .header("X-Auth-Role", "ADMIN")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        org.mockito.ArgumentCaptor<ServerWebExchange> captor =
                org.mockito.ArgumentCaptor.forClass(ServerWebExchange.class);

        filter.filter(exchange, chain).block();

        verify(chain).filter(captor.capture());
        ServerWebExchange captured = captor.getValue();
        org.junit.jupiter.api.Assertions.assertNull(captured.getRequest().getHeaders().getFirst("X-Auth-User"));
        org.junit.jupiter.api.Assertions.assertNull(captured.getRequest().getHeaders().getFirst("X-Auth-Role"));
    }
}
