package com.payvault.auth.service;

import com.payvault.auth.dto.LoginRequest;
import com.payvault.auth.dto.SignupRequest;

public interface AuthService {

    String signup(SignupRequest request);

    String login(LoginRequest request);
}