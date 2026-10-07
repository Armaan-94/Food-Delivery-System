package com.fooddelivery.deliveryservice.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.common.exception.ValidationException;
import com.fooddelivery.deliveryservice.client.OrderClient;
import com.fooddelivery.deliveryservice.dto.DeliveryDto;
import com.fooddelivery.deliveryservice.dto.DeliveryRequestDto;
import com.fooddelivery.deliveryservice.dto.DeliveryStatusUpdateDto;
import com.fooddelivery.deliveryservice.model.Delivery;
import com.fooddelivery.deliveryservice.model.DeliveryStatus;
import com.fooddelivery.deliveryservice.model.Partner;
import com.fooddelivery.deliveryservice.repository.DeliveryRepository;
import com.fooddelivery.deliveryservice.repository.PartnerRepository;

@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final PartnerRepository partnerRepository;
    private final OrderClient orderClient;

    public DeliveryService(DeliveryRepository deliveryRepository, PartnerRepository partnerRepository,
            OrderClient orderClient) {
        this.deliveryRepository = deliveryRepository;
        this.partnerRepository = partnerRepository;
        this.orderClient = orderClient;
    }

    /** Not transactional on purpose: the remote order check must not hold a database connection. */
    public DeliveryDto createDelivery(DeliveryRequestDto request) {
        orderClient.assertOrderExists(request.orderId());
        if (request.partnerId() != null) {
            requireAvailablePartner(request.partnerId());
        }

        Delivery delivery = new Delivery();
        delivery.setOrderId(request.orderId());
        delivery.setPartnerId(request.partnerId());
        delivery.setStatus(request.partnerId() != null ? DeliveryStatus.ASSIGNED : DeliveryStatus.PENDING);
        delivery.setPickupLocation(request.pickupLocation().trim());
        delivery.setDropoffLocation(request.dropoffLocation().trim());

        long id = deliveryRepository.insert(delivery);
        return DeliveryDto.from(findOrThrow(id));
    }

    @Transactional
    public DeliveryDto updateDeliveryStatus(long id, DeliveryStatusUpdateDto request) {
        Delivery delivery = findOrThrow(id);
        DeliveryStatus target = DeliveryStatus.parse(request.status());

        if (!delivery.getStatus().allowedNext().contains(target)) {
            throw new ValidationException("A delivery cannot move from " + delivery.getStatus() + " to " + target + ".");
        }

        Long partnerId = delivery.getPartnerId();
        if (request.partnerId() != null && !request.partnerId().equals(partnerId)) {
            requireAvailablePartner(request.partnerId());
            partnerId = request.partnerId();
        }
        if (target.requiresPartner() && partnerId == null) {
            throw new ValidationException("A partner must be assigned before the delivery can be " + target + ".");
        }

        if (!deliveryRepository.updateStatus(id, target, partnerId)) {
            throw notFound(id);
        }
        return DeliveryDto.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public DeliveryDto getDeliveryById(long id) {
        return DeliveryDto.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<DeliveryDto> getAllDeliveries() {
        return deliveryRepository.findAll().stream().map(DeliveryDto::from).toList();
    }

    @Transactional
    public void deleteDelivery(long id) {
        if (!deliveryRepository.deleteById(id)) {
            throw notFound(id);
        }
    }

    private void requireAvailablePartner(long partnerId) {
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found with id: " + partnerId));
        if (!partner.isAvailable()) {
            throw new ValidationException("Partner " + partnerId + " is not available.");
        }
    }

    private Delivery findOrThrow(long id) {
        return deliveryRepository.findById(id).orElseThrow(() -> notFound(id));
    }

    private static ResourceNotFoundException notFound(long id) {
        return new ResourceNotFoundException("Delivery not found with id: " + id);
    }
}
