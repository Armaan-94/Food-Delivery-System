package com.fooddelivery.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fooddelivery.dto.DeliveryRequestDTO;
import com.fooddelivery.dto.DeliveryResponseDTO;
import com.fooddelivery.dto.DeliveryStatusResponseDto;
import com.fooddelivery.dto.DeliveryStatusUpdateDTO;
import com.fooddelivery.exceptions.DatabaseAccessException;
import com.fooddelivery.exceptions.ResourceNotFoundException;
import com.fooddelivery.exceptions.ServiceUnavailableException;
import com.fooddelivery.exceptions.ValidationException;
import com.fooddelivery.service.DeliveryService;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {

    @Autowired
    private DeliveryService deliveryService;

    @PostMapping
    public ResponseEntity<DeliveryStatusResponseDto> createDelivery(@RequestBody DeliveryRequestDTO request) {
        try {
            DeliveryResponseDTO created = deliveryService.createDelivery(request);
            DeliveryStatusResponseDto response = new DeliveryStatusResponseDto("Success: Delivery created.", created);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (ValidationException e) {
            return new ResponseEntity<>(new DeliveryStatusResponseDto("Error: " + e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(new DeliveryStatusResponseDto("Error: " + e.getMessage()), HttpStatus.NOT_FOUND);
        } catch (ServiceUnavailableException e) {
            return new ResponseEntity<>(new DeliveryStatusResponseDto("Error: " + e.getMessage()), HttpStatus.SERVICE_UNAVAILABLE);
        } catch (DatabaseAccessException e) {
            return new ResponseEntity<>(new DeliveryStatusResponseDto("Error: Database operation failed - " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>(new DeliveryStatusResponseDto("Error: An unexpected error occurred - " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<DeliveryStatusResponseDto> getAllDeliveries() {
        try {
            List<DeliveryResponseDTO> deliveries = deliveryService.getAllDeliveries();
            DeliveryStatusResponseDto response = new DeliveryStatusResponseDto("Success: Retrieved all deliveries.", deliveries, true);
            return ResponseEntity.ok(response);
        } catch (DatabaseAccessException e) {
            return new ResponseEntity<>(new DeliveryStatusResponseDto("Error: Database operation failed - " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>(new DeliveryStatusResponseDto("Error: An unexpected error occurred - " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeliveryStatusResponseDto> getDeliveryById(@PathVariable Long id) {
        try {
            DeliveryResponseDTO delivery = deliveryService.getDeliveryById(id);
            DeliveryStatusResponseDto response = new DeliveryStatusResponseDto("Success: Retrieved delivery.", delivery);
            return ResponseEntity.ok(response);
        } catch (ValidationException e) {
            return new ResponseEntity<>(new DeliveryStatusResponseDto("Error: " + e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(new DeliveryStatusResponseDto("Error: " + e.getMessage()), HttpStatus.NOT_FOUND);
        } catch (DatabaseAccessException e) {
            return new ResponseEntity<>(new DeliveryStatusResponseDto("Error: Database operation failed - " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>(new DeliveryStatusResponseDto("Error: An unexpected error occurred - " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<DeliveryStatusResponseDto> updateDeliveryStatus(
            @PathVariable Long id,
            @RequestBody DeliveryStatusUpdateDTO request) {
        try {
            DeliveryResponseDTO updated = deliveryService.updateDeliveryStatus(id, request);
            DeliveryStatusResponseDto response = new DeliveryStatusResponseDto("Success: Delivery status updated.", updated);
            return ResponseEntity.ok(response);
        } catch (ValidationException e) {
            return new ResponseEntity<>(new DeliveryStatusResponseDto("Error: " + e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(new DeliveryStatusResponseDto("Error: " + e.getMessage()), HttpStatus.NOT_FOUND);
        } catch (DatabaseAccessException e) {
            return new ResponseEntity<>(new DeliveryStatusResponseDto("Error: Database operation failed - " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>(new DeliveryStatusResponseDto("Error: An unexpected error occurred - " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DeliveryStatusResponseDto> deleteDelivery(@PathVariable Long id) {
        try {
            deliveryService.deleteDelivery(id);
            return ResponseEntity.ok(new DeliveryStatusResponseDto("Success: Delivery deleted."));
        } catch (ValidationException e) {
            return new ResponseEntity<>(new DeliveryStatusResponseDto("Error: " + e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(new DeliveryStatusResponseDto("Error: " + e.getMessage()), HttpStatus.NOT_FOUND);
        } catch (DatabaseAccessException e) {
            return new ResponseEntity<>(new DeliveryStatusResponseDto("Error: Database operation failed - " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>(new DeliveryStatusResponseDto("Error: An unexpected error occurred - " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
