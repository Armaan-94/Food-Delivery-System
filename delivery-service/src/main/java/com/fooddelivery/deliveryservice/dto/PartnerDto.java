package com.fooddelivery.deliveryservice.dto;

import com.fooddelivery.deliveryservice.model.Partner;

public record PartnerDto(Long id, String name, String phoneNumber, String vehicleType, boolean available) {

    public static PartnerDto from(Partner partner) {
        return new PartnerDto(partner.getId(), partner.getName(), partner.getPhoneNumber(), partner.getVehicleType(),
                partner.isAvailable());
    }
}
