package com.fooddelivery.service;

import java.util.List;

import com.fooddelivery.dto.DeliveryRequestDTO;
import com.fooddelivery.dto.DeliveryResponseDTO;
import com.fooddelivery.dto.DeliveryStatusUpdateDTO;

public interface DeliveryService {
    DeliveryResponseDTO createDelivery(DeliveryRequestDTO request);
    DeliveryResponseDTO updateDeliveryStatus(Long deliveryId, DeliveryStatusUpdateDTO request);
    DeliveryResponseDTO getDeliveryById(Long deliveryId);
    List<DeliveryResponseDTO> getAllDeliveries();
    void deleteDelivery(Long deliveryId);
}
