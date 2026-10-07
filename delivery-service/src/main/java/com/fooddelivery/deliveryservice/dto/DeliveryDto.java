package com.fooddelivery.deliveryservice.dto;

import java.time.LocalDateTime;

import com.fooddelivery.deliveryservice.model.Delivery;
import com.fooddelivery.deliveryservice.model.DeliveryStatus;

public record DeliveryDto(
        Long id,
        Long orderId,
        Long partnerId,
        DeliveryStatus status,
        String pickupLocation,
        String dropoffLocation,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static DeliveryDto from(Delivery delivery) {
        return new DeliveryDto(delivery.getId(), delivery.getOrderId(), delivery.getPartnerId(), delivery.getStatus(),
                delivery.getPickupLocation(), delivery.getDropoffLocation(), delivery.getCreatedAt(),
                delivery.getUpdatedAt());
    }
}
