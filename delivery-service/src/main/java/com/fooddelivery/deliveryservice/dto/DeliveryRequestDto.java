package com.fooddelivery.deliveryservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Giving a partnerId assigns the delivery immediately; otherwise it starts as PENDING. */
public record DeliveryRequestDto(
        @NotNull @Positive Long orderId,
        @Positive Long partnerId,
        @NotBlank @Size(max = 255) String pickupLocation,
        @NotBlank @Size(max = 255) String dropoffLocation) {
}
