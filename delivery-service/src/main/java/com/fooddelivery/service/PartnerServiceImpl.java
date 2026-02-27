package com.fooddelivery.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fooddelivery.dto.DeliveryPersonDTO;
import com.fooddelivery.exceptions.ValidationException;
import com.fooddelivery.mapper.ModelMapperUtil;
import com.fooddelivery.model.Partner;
import com.fooddelivery.repository.PartnerRepository;

@Service
public class PartnerServiceImpl implements PartnerService {

    private final PartnerRepository partnerRepository;

    public PartnerServiceImpl(PartnerRepository partnerRepository) {
        this.partnerRepository = partnerRepository;
    }

    @Override
    public DeliveryPersonDTO registerPartner(DeliveryPersonDTO request) {
        validatePartnerDto(request);
        Partner partner = ModelMapperUtil.mapPartnerDTOToEntity(request);
        partnerRepository.save(partner);
        return ModelMapperUtil.mapPartnerToDTO(partner);
    }

    @Override
    public DeliveryPersonDTO updatePartner(Long partnerId, DeliveryPersonDTO request) {
        if (partnerId == null || partnerId <= 0) throw new ValidationException("Invalid Partner ID.");
        validatePartnerDto(request);
        Partner existing = partnerRepository.findById(partnerId.intValue());
        existing.setName(request.getName());
        existing.setVehicle(request.getVehicleType());
        existing.setAvailable(true);
        partnerRepository.update(existing);
        return ModelMapperUtil.mapPartnerToDTO(existing);
    }

    @Override
    public DeliveryPersonDTO getPartnerById(Long partnerId) {
        if (partnerId == null || partnerId <= 0) throw new ValidationException("Invalid Partner ID.");
        Partner partner = partnerRepository.findById(partnerId.intValue());
        return ModelMapperUtil.mapPartnerToDTO(partner);
    }

    @Override
    public List<DeliveryPersonDTO> getAllPartners() {
        return partnerRepository.findAll()
                .stream()
                .map(ModelMapperUtil::mapPartnerToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deletePartner(Long partnerId) {
        if (partnerId == null || partnerId <= 0) throw new ValidationException("Invalid Partner ID.");
        partnerRepository.delete(partnerId.intValue());
    }

    private void validatePartnerDto(DeliveryPersonDTO dto) {
        if (dto == null) throw new ValidationException("Partner request cannot be null.");
        if (dto.getName() == null || dto.getName().trim().isEmpty()) throw new ValidationException("Partner name cannot be empty.");
        if (dto.getVehicleType() == null || dto.getVehicleType().trim().isEmpty()) throw new ValidationException("Partner vehicle type cannot be empty.");
    }
}
