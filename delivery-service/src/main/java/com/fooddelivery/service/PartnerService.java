package com.fooddelivery.service;

import java.util.List;

import com.fooddelivery.dto.DeliveryPersonDTO;

public interface PartnerService {
    DeliveryPersonDTO registerPartner(DeliveryPersonDTO request);
    DeliveryPersonDTO updatePartner(Long partnerId, DeliveryPersonDTO request);
    DeliveryPersonDTO getPartnerById(Long partnerId);
    List<DeliveryPersonDTO> getAllPartners();
    void deletePartner(Long partnerId);
}
