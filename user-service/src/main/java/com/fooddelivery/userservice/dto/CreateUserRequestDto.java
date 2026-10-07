package com.fooddelivery.userservice.dto;

import com.fooddelivery.common.security.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Administrator-created account. The role defaults to USER. */
public record CreateUserRequestDto(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        Role role) {
}
