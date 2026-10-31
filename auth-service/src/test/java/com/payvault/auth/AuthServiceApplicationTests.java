package com.payvault.auth;

import com.payvault.auth.dto.LoginRequest;
import com.payvault.auth.dto.SignupRequest;
import com.payvault.auth.entity.AuthUser;
import com.payvault.auth.repository.AuthUserRepository;
import com.payvault.auth.service.AuthServiceImpl;
import com.payvault.auth.util.JwtUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceApplicationTests {

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {

        MockitoAnnotations.openMocks(this);

        authService = new AuthServiceImpl(
                authUserRepository,
                passwordEncoder,
                jwtUtil
        );
    }

    // ---------------------------------------------------------
    // 1. Signup - Success
    // ---------------------------------------------------------
    @Test
    void signupShouldRegisterUserSuccessfully() {
        SignupRequest request =
                new SignupRequest(
                        "testuser",
                        "Test@123"
                );

        when(authUserRepository.existsByUsername("testuser"))
                .thenReturn(false);

        when(passwordEncoder.encode("Test@123"))
                .thenReturn("encoded-password");

        AuthUser savedUser =
                new AuthUser(
                        "testuser",
                        "encoded-password",
                        "USER"
                );

        when(authUserRepository.save(any(AuthUser.class)))
                .thenReturn(savedUser);

        String response =
                authService.signup(request);

        assertEquals(
                "User registered successfully",
                response
        );

        verify(authUserRepository)
                .existsByUsername("testuser");

        verify(passwordEncoder)
                .encode("Test@123");

        verify(authUserRepository)
                .save(any(AuthUser.class));
    }

    // ---------------------------------------------------------
    // 2. Signup - Duplicate Username
    // ---------------------------------------------------------

    @Test
    void signupShouldRejectDuplicateUsername() {

        SignupRequest request =
                new SignupRequest(
                        "existinguser",
                        "Test@123"
                );

        when(authUserRepository.existsByUsername(
                "existinguser"
        )).thenReturn(true);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> authService.signup(request)
                );

        assertEquals(
                "Username already exists",
                exception.getMessage()
        );

        verify(authUserRepository)
                .existsByUsername("existinguser");

        verify(passwordEncoder, never())
                .encode(anyString());

        verify(authUserRepository, never())
                .save(any(AuthUser.class));
    }

    // ---------------------------------------------------------
    // 3. Login - Success
    // ---------------------------------------------------------

    @Test
    void loginShouldReturnJwtToken() {

        LoginRequest request =
                new LoginRequest(
                        "testuser",
                        "Test@123"
                );

        AuthUser user =
                new AuthUser(
                        "testuser",
                        "encoded-password",
                        "USER"
                );

        when(authUserRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "Test@123",
                "encoded-password"
        )).thenReturn(true);

        when(jwtUtil.generateToken(
                "testuser",
                "USER"
        )).thenReturn("mock-jwt-token");

        String token =
                authService.login(request);

        assertEquals(
                "mock-jwt-token",
                token
        );

        verify(authUserRepository)
                .findByUsername("testuser");

        verify(passwordEncoder)
                .matches(
                        "Test@123",
                        "encoded-password"
                );

        verify(jwtUtil)
                .generateToken(
                        "testuser",
                        "USER"
                );
    }

    // ---------------------------------------------------------
    // 4. Login - Username Not Found
    // ---------------------------------------------------------

    @Test
    void loginShouldRejectUnknownUsername() {

        LoginRequest request =
                new LoginRequest(
                        "unknownuser",
                        "Test@123"
                );

        when(authUserRepository.findByUsername(
                "unknownuser"
        )).thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> authService.login(request)
                );

        assertEquals(
                "Invalid username or password",
                exception.getMessage()
        );

        verify(passwordEncoder, never())
                .matches(
                        anyString(),
                        anyString()
                );

        verify(jwtUtil, never())
                .generateToken(
                        anyString(),
                        anyString()
                );
    }

    // ---------------------------------------------------------
    // 5. Login - Wrong Password
    // ---------------------------------------------------------

    @Test
    void loginShouldRejectWrongPassword() {

        LoginRequest request =
                new LoginRequest(
                        "testuser",
                        "WrongPassword"
                );

        AuthUser user =
                new AuthUser(
                        "testuser",
                        "encoded-password",
                        "USER"
                );

        when(authUserRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "WrongPassword",
                "encoded-password"
        )).thenReturn(false);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> authService.login(request)
                );

        assertEquals(
                "Invalid username or password",
                exception.getMessage()
        );

        verify(jwtUtil, never())
                .generateToken(
                        anyString(),
                        anyString()
                );
    }

    // ---------------------------------------------------------
    // 6. JWT - Generate Token
    // ---------------------------------------------------------

    @Test
    void jwtShouldGenerateValidToken() {

        JwtUtil realJwtUtil = new JwtUtil();

        String token =
                realJwtUtil.generateToken(
                        "testuser",
                        "USER"
                );

        assertNotNull(token);

        assertTrue(
                realJwtUtil.isTokenValid(token)
        );
    }

    // ---------------------------------------------------------
    // 7. JWT - Extract Username
    // ---------------------------------------------------------

    @Test
    void jwtShouldExtractUsername() {

        JwtUtil realJwtUtil = new JwtUtil();

        String token =
                realJwtUtil.generateToken(
                        "testuser",
                        "USER"
                );

        String username =
                realJwtUtil.extractUsername(token);

        assertEquals(
                "testuser",
                username
        );
    }

    // ---------------------------------------------------------
    // 8. JWT - Extract Role
    // ---------------------------------------------------------

    @Test
    void jwtShouldExtractRole() {

        JwtUtil realJwtUtil = new JwtUtil();

        String token =
                realJwtUtil.generateToken(
                        "testuser",
                        "USER"
                );

        String role =
                realJwtUtil.extractRole(token);

        assertEquals(
                "USER",
                role
        );
    }

    // ---------------------------------------------------------
    // 9. JWT - Invalid Token
    // ---------------------------------------------------------

    @Test
    void jwtShouldRejectInvalidToken() {

        JwtUtil realJwtUtil = new JwtUtil();

        String invalidToken =
                "invalid.jwt.token";

        assertFalse(
                realJwtUtil.isTokenValid(
                        invalidToken
                )
        );
    }

    // ---------------------------------------------------------
    // 10. JWT - Expired Token Validation
    // ---------------------------------------------------------

    @Test
    void jwtShouldRejectExpiredToken() {

        // Instantiate JwtUtil with negative expiration (-5000ms)
        JwtUtil expiredJwtUtil = new JwtUtil(
                JwtUtil.DEFAULT_TEST_SECRET,
                -5000L
        );

        String expiredToken = expiredJwtUtil.generateToken("testuser", "USER");
        assertFalse(expiredJwtUtil.isTokenValid(expiredToken));
    }

    // ---------------------------------------------------------
    // 11. JWT - Custom Secret Key Support
    // ---------------------------------------------------------

    @Test
    void jwtShouldSupportCustomSecretKey() {

        String customSecret = "CustomSuperSecretKeyForPayVaultApplicationTests2026";
        JwtUtil customJwtUtil = new JwtUtil(customSecret, 60000L);

        String token = customJwtUtil.generateToken("customuser", "ADMIN");
        assertTrue(customJwtUtil.isTokenValid(token));
        assertEquals("customuser", customJwtUtil.extractUsername(token));
        assertEquals("ADMIN", customJwtUtil.extractRole(token));

        // Token signed with default key should fail on custom secret
        JwtUtil defaultJwtUtil = new JwtUtil();
        String defaultToken = defaultJwtUtil.generateToken("customuser", "ADMIN");
        assertFalse(customJwtUtil.isTokenValid(defaultToken));
    }

    // ---------------------------------------------------------
    // 12. JWT - Reject Null and Empty Tokens
    // ---------------------------------------------------------

    @Test
    void jwtShouldRejectNullOrEmptyToken() {

        JwtUtil realJwtUtil = new JwtUtil();

        assertFalse(realJwtUtil.isTokenValid(null));
        assertFalse(realJwtUtil.isTokenValid(""));
        assertFalse(realJwtUtil.isTokenValid("   "));
    }

    // ---------------------------------------------------------
    // 13. JWT - Reject Insecure / Missing Secret
    // ---------------------------------------------------------

    @Test
    void jwtShouldRejectShortOrMissingSecret() {

        assertThrows(IllegalArgumentException.class, () -> new JwtUtil(null, 3600000L));
        assertThrows(IllegalArgumentException.class, () -> new JwtUtil("   ", 3600000L));
        assertThrows(IllegalArgumentException.class, () -> new JwtUtil("short-secret-under-32-chars", 3600000L));
    }
}