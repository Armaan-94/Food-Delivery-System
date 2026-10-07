package com.fooddelivery.deliveryservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** The partnerId is required when moving to ASSIGNED and the delivery has no partner yet. */
public record DeliveryStatusUpdateDto(@NotBlank String status, @Positive Long partnerId) {
}
