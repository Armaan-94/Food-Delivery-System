package com.fooddelivery.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.fooddelivery.dto.DeliveryRequestDTO;
import com.fooddelivery.dto.DeliveryResponseDTO;
import com.fooddelivery.dto.DeliveryStatusUpdateDTO;
import com.fooddelivery.dto.UserDTO;
import com.fooddelivery.exceptions.ResourceNotFoundException;
import com.fooddelivery.exceptions.ServiceUnavailableException;
import com.fooddelivery.exceptions.ValidationException;
import com.fooddelivery.mapper.ModelMapperUtil;
import com.fooddelivery.model.Delivery;
import com.fooddelivery.repository.DeliveryRepository;

@Service
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final RestTemplate restTemplate;

    @Autowired
    public DeliveryServiceImpl(DeliveryRepository deliveryRepository, RestTemplate restTemplate) {
        this.deliveryRepository = deliveryRepository;
        this.restTemplate = restTemplate;
    }

    @Override
    public DeliveryResponseDTO createDelivery(DeliveryRequestDTO request) {
        validateDeliveryRequest(request);
        UserDTO user = getUserById(request.getDeliveryPersonId());

        Delivery delivery = new Delivery();
        delivery.setOrderId(request.getOrderId().intValue());
        delivery.setUserId(request.getDeliveryPersonId().intValue());
        delivery.setStatus("PENDING");
        delivery.setAssignedPartnerId(null);

        deliveryRepository.save(delivery);
        return ModelMapperUtil.mapDeliveryToDTO(delivery);
    }

    private UserDTO getUserById(Long userId) {
        String userServiceUrl = "http://user-service/api/users/" + userId;
        try {
            UserDTO user = restTemplate.getForObject(userServiceUrl, UserDTO.class);
            if (user == null) {
                throw new ResourceNotFoundException("Delivery person not found with id: " + userId);
            }
            return user;
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Delivery person not found with id: " + userId);
        } catch (ResourceAccessException e) {
            throw new ServiceUnavailableException("User service is currently unavailable.", e);
        } catch (Exception e) {
            throw new ServiceUnavailableException("Failed to communicate with user-service.", e);
        }
    }

    @Override
    public DeliveryResponseDTO updateDeliveryStatus(Long deliveryId, DeliveryStatusUpdateDTO request) {
        if (deliveryId == null || deliveryId <= 0) {
            throw new ValidationException("Invalid delivery ID provided.");
        }
        validateStatusUpdate(request);

        Delivery existingDelivery = deliveryRepository.findById(deliveryId.intValue());
        deliveryRepository.updateStatus(
                deliveryId.intValue(),
                request.getStatus(),
                existingDelivery.getAssignedPartnerId()
        );

        Delivery updatedDelivery = deliveryRepository.findById(deliveryId.intValue());
        return ModelMapperUtil.mapDeliveryToDTO(updatedDelivery);
    }

    @Override
    public DeliveryResponseDTO getDeliveryById(Long deliveryId) {
        if (deliveryId == null || deliveryId <= 0) {
            throw new ValidationException("Invalid delivery ID provided.");
        }
        Delivery delivery = deliveryRepository.findById(deliveryId.intValue());
        return ModelMapperUtil.mapDeliveryToDTO(delivery);
    }

    @Override
    public List<DeliveryResponseDTO> getAllDeliveries() {
        return deliveryRepository.findAll()
                .stream()
                .map(ModelMapperUtil::mapDeliveryToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteDelivery(Long deliveryId) {
        if (deliveryId == null || deliveryId <= 0) {
            throw new ValidationException("Invalid delivery ID provided.");
        }
        deliveryRepository.delete(deliveryId.intValue());
    }

    private void validateDeliveryRequest(DeliveryRequestDTO request) {
        if (request == null) throw new ValidationException("Delivery request cannot be null.");
        if (request.getOrderId() == null || request.getOrderId() <= 0) throw new ValidationException("Invalid Order ID.");
        if (request.getDeliveryPersonId() == null || request.getDeliveryPersonId() <= 0) throw new ValidationException("Invalid Delivery Person ID.");
        if (request.getPickupLocation() == null || request.getPickupLocation().trim().isEmpty()) throw new ValidationException("Pickup location cannot be empty.");
        if (request.getDropoffLocation() == null || request.getDropoffLocation().trim().isEmpty()) throw new ValidationException("Dropoff location cannot be empty.");
    }

    private void validateStatusUpdate(DeliveryStatusUpdateDTO request) {
        if (request == null) throw new ValidationException("Status update request cannot be null.");
        if (request.getStatus() == null || request.getStatus().trim().isEmpty()) throw new ValidationException("Status cannot be empty.");
    }
}
