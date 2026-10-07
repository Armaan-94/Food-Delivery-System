package com.fooddelivery.userservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fooddelivery.common.security.AuthenticatedUser;
import com.fooddelivery.common.web.ApiResponse;
import com.fooddelivery.userservice.dto.CreateUserRequestDto;
import com.fooddelivery.userservice.dto.UpdateUserRequestDto;
import com.fooddelivery.userservice.dto.UserDto;
import com.fooddelivery.userservice.service.UserService;

import jakarta.validation.Valid;

/** Administrators manage every account; everyone else can only read, change or delete their own. */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final String ADMIN_OR_SELF = "hasRole('ADMIN') or authentication.name == #id.toString()";

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody CreateUserRequestDto request) {
        UserDto created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("User created.", created));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserDto>> getAllUsers() {
        return ApiResponse.ok(userService.getAllUsers());
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<UserDto> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(userService.getUserById(AuthenticatedUser.from(jwt).id()));
    }

    @GetMapping("/{id}")
    @PreAuthorize(ADMIN_OR_SELF)
    public ApiResponse<UserDto> getUserById(@PathVariable Long id) {
        return ApiResponse.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize(ADMIN_OR_SELF)
    public ApiResponse<UserDto> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok("User updated.", userService.updateUser(id, request, AuthenticatedUser.from(jwt)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(ADMIN_OR_SELF)
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.message("User deleted.");
    }
}
