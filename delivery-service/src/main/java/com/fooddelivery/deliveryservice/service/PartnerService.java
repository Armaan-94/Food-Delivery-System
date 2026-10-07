package com.fooddelivery.deliveryservice.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.deliveryservice.dto.PartnerDto;
import com.fooddelivery.deliveryservice.dto.PartnerRequestDto;
import com.fooddelivery.deliveryservice.model.Partner;
import com.fooddelivery.deliveryservice.repository.PartnerRepository;

@Service
public class PartnerService {

    private final PartnerRepository partnerRepository;

    public PartnerService(PartnerRepository partnerRepository) {
        this.partnerRepository = partnerRepository;
    }

    public PartnerDto createPartner(PartnerRequestDto request) {
        Partner partner = new Partner();
        apply(partner, request, true);
        long id = partnerRepository.insert(partner);
        return PartnerDto.from(findOrThrow(id));
    }

    public PartnerDto updatePartner(long id, PartnerRequestDto request) {
        Partner partner = findOrThrow(id);
        apply(partner, request, partner.isAvailable());
        if (!partnerRepository.update(partner)) {
            throw notFound(id);
        }
        return PartnerDto.from(partner);
    }

    public PartnerDto getPartnerById(long id) {
        return PartnerDto.from(findOrThrow(id));
    }

    public List<PartnerDto> getAllPartners() {
        return partnerRepository.findAll().stream().map(PartnerDto::from).toList();
    }

    public void deletePartner(long id) {
        if (!partnerRepository.deleteById(id)) {
            throw notFound(id);
        }
    }

    private static void apply(Partner partner, PartnerRequestDto request, boolean defaultAvailable) {
        partner.setName(request.name().trim());
        partner.setPhoneNumber(request.phoneNumber() == null || request.phoneNumber().isBlank()
                ? null
                : request.phoneNumber().trim());
        partner.setVehicleType(request.vehicleType().trim());
        partner.setAvailable(request.available() != null ? request.available() : defaultAvailable);
    }

    private Partner findOrThrow(long id) {
        return partnerRepository.findById(id).orElseThrow(() -> notFound(id));
    }

    private static ResourceNotFoundException notFound(long id) {
        return new ResourceNotFoundException("Partner not found with id: " + id);
    }
}
