package com.fooddelivery.authservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fooddelivery.authservice.dto.LoginRequestDto;
import com.fooddelivery.authservice.dto.SignupRequestDto;
import com.fooddelivery.authservice.dto.TokenResponseDto;
import com.fooddelivery.authservice.dto.UserInfoDto;
import com.fooddelivery.authservice.service.AuthService;
import com.fooddelivery.common.web.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserInfoDto>> signup(@Valid @RequestBody SignupRequestDto request) {
        UserInfoDto user = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Account created.", user));
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ApiResponse.ok(authService.login(request));
    }
}
