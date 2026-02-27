package com.fooddelivery.mapper;

import java.time.LocalDateTime;

import com.fooddelivery.dto.DeliveryPersonDTO;
import com.fooddelivery.dto.DeliveryResponseDTO;
import com.fooddelivery.model.Delivery;
import com.fooddelivery.model.Partner;

public class ModelMapperUtil {

    // Delivery → DeliveryResponseDTO
    public static DeliveryResponseDTO mapDeliveryToDTO(Delivery delivery) {
        DeliveryResponseDTO dto = new DeliveryResponseDTO();
        dto.setId((long) delivery.getId());
        dto.setOrderId((long) delivery.getOrderId());
        dto.setDeliveryPersonId((long) delivery.getUserId());
        dto.setStatus(delivery.getStatus());

        // Optional fields – for now we can keep null or set defaults
        dto.setPickupLocation(null);
        dto.setDropoffLocation(null);
        dto.setCreatedAt(LocalDateTime.now());  // Example of setting timestamp

        return dto;
    }

    // DeliveryPersonDTO → Partner (for saving/updating Partner)
    public static Partner mapPartnerDTOToEntity(DeliveryPersonDTO dto) {
        Partner partner = new Partner();
        partner.setId(dto.getId() != null ? dto.getId().intValue() : 0);
        partner.setName(dto.getName());
        partner.setVehicle(dto.getVehicleType());
        partner.setAvailable(true);  // Default to available
        return partner;
    }

    // Partner → DeliveryPersonDTO
    public static DeliveryPersonDTO mapPartnerToDTO(Partner partner) {
        DeliveryPersonDTO dto = new DeliveryPersonDTO();
        dto.setId((long) partner.getId());
        dto.setName(partner.getName());
        dto.setPhoneNumber(null);  // No phoneNumber in model
        dto.setVehicleType(partner.getVehicle());
        return dto;
    }
}
