package com.fooddelivery.authservice.service;

import org.springframework.stereotype.Service;

import com.fooddelivery.authservice.client.UserServiceClient;
import com.fooddelivery.authservice.dto.LoginRequestDto;
import com.fooddelivery.authservice.dto.SignupRequestDto;
import com.fooddelivery.authservice.dto.TokenResponseDto;
import com.fooddelivery.authservice.dto.UserInfoDto;
import com.fooddelivery.common.exception.AuthenticationFailedException;
import com.fooddelivery.common.security.JwtTokenService;

@Service
public class AuthService {

    private final UserServiceClient userServiceClient;
    private final JwtTokenService tokenService;
    private final LoginAttemptService loginAttempts;

    public AuthService(UserServiceClient userServiceClient, JwtTokenService tokenService,
            LoginAttemptService loginAttempts) {
        this.userServiceClient = userServiceClient;
        this.tokenService = tokenService;
        this.loginAttempts = loginAttempts;
    }

    public UserInfoDto signup(SignupRequestDto request) {
        return userServiceClient.register(request);
    }

    public TokenResponseDto login(LoginRequestDto request) {
        loginAttempts.assertNotLocked(request.email());

        UserInfoDto user;
        try {
            user = userServiceClient.verify(request);
        } catch (AuthenticationFailedException e) {
            loginAttempts.recordFailure(request.email());
            throw e;
        }

        loginAttempts.recordSuccess(request.email());
        String token = tokenService.issueUserToken(user.id(), user.email(), user.name(), user.role());
        return TokenResponseDto.bearer(token, tokenService.accessTokenTtlSeconds(), user);
    }
}
