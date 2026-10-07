package com.fooddelivery.deliveryservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** The available flag is optional and defaults to true. */
public record PartnerRequestDto(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 20) @Pattern(regexp = "^[0-9+()\\-\\s]*$", message = "must contain only digits, spaces and + ( ) -") String phoneNumber,
        @NotBlank @Size(max = 50) String vehicleType,
        Boolean available) {
}
