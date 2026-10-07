package com.fooddelivery.deliveryservice.model;

import java.util.Arrays;
import java.util.Set;

import com.fooddelivery.common.exception.ValidationException;

public enum DeliveryStatus {
    PENDING,
    ASSIGNED,
    PICKED_UP,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED;

    /** The statuses a delivery may move to from this one. */
    public Set<DeliveryStatus> allowedNext() {
        return switch (this) {
            case PENDING -> Set.of(ASSIGNED, CANCELLED);
            case ASSIGNED -> Set.of(PICKED_UP, CANCELLED);
            case PICKED_UP -> Set.of(OUT_FOR_DELIVERY);
            case OUT_FOR_DELIVERY -> Set.of(DELIVERED);
            case DELIVERED, CANCELLED -> Set.of();
        };
    }

    /** A delivery in any of these states must have a partner. */
    public boolean requiresPartner() {
        return this != PENDING && this != CANCELLED;
    }

    public static DeliveryStatus parse(String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Status cannot be empty.");
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Unknown status '" + value + "'. Allowed: " + Arrays.toString(values()) + ".");
        }
    }
}
