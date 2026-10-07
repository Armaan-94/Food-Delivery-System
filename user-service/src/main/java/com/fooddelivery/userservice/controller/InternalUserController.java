package com.fooddelivery.userservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fooddelivery.common.web.ApiResponse;
import com.fooddelivery.userservice.dto.RegisterUserRequestDto;
import com.fooddelivery.userservice.dto.UserDto;
import com.fooddelivery.userservice.dto.VerifyCredentialsRequestDto;
import com.fooddelivery.userservice.service.UserService;

import jakarta.validation.Valid;

/**
 * Called only by auth-service with a service token. The gateway has no route to /internal, and the
 * SERVICE role is required here as well.
 */
@RestController
@RequestMapping("/internal/users")
@PreAuthorize("hasRole('SERVICE')")
public class InternalUserController {

    private final UserService userService;

    public InternalUserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDto>> register(@Valid @RequestBody RegisterUserRequestDto request) {
        UserDto created = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("User registered.", created));
    }

    @PostMapping("/verify")
    public ApiResponse<UserDto> verify(@Valid @RequestBody VerifyCredentialsRequestDto request) {
        return ApiResponse.ok(userService.verifyCredentials(request.email(), request.password()));
    }
}
