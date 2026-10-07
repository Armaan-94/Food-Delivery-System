package com.fooddelivery.userservice.dto;

import jakarta.validation.constraints.NotBlank;

/** Sent by auth-service on login. */
public record VerifyCredentialsRequestDto(@NotBlank String email, @NotBlank String password) {
}
