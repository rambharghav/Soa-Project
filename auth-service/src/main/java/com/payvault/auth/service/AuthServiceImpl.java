package com.payvault.auth.service;

import com.payvault.auth.dto.LoginRequest;
import com.payvault.auth.dto.SignupRequest;
import com.payvault.auth.entity.AuthUser;
import com.payvault.auth.repository.AuthUserRepository;
import com.payvault.auth.util.JwtUtil;
import com.payvault.auth.config.*;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(
            AuthUserRepository authUserRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil
    ) {
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public String signup(SignupRequest request) {

        if (authUserRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException(
                    "Username already exists"
            );
        }

        String encodedPassword =
                passwordEncoder.encode(request.getPassword());

        AuthUser user = new AuthUser(
                request.getUsername(),
                encodedPassword,
                "USER"
        );

        authUserRepository.save(user);

        return "User registered successfully";
    }

    @Override
    public String login(LoginRequest request) {

        AuthUser user = authUserRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid username or password"
                        )
                );

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {
            throw new IllegalArgumentException(
                    "Invalid username or password"
            );
        }

        return jwtUtil.generateToken(
                user.getUsername(),
                user.getRole()
        );
    }
}