package com.fooddelivery.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fooddelivery.dto.DeliveryPersonDTO;
import com.fooddelivery.dto.DeliveryStatusResponseDto;
import com.fooddelivery.exceptions.DatabaseAccessException;
import com.fooddelivery.exceptions.ResourceNotFoundException;
import com.fooddelivery.exceptions.ValidationException;
import com.fooddelivery.service.PartnerService;

@RestController
@RequestMapping("/api/partners")
public class PartnerController {

    @Autowired
    private PartnerService partnerService;

    @PostMapping
    public ResponseEntity<DeliveryStatusResponseDto> createPartner(@RequestBody DeliveryPersonDTO request) {
        try {
            DeliveryPersonDTO created = partnerService.registerPartner(request);
            DeliveryStatusResponseDto response = new DeliveryStatusResponseDto("Success: Partner created.", created);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (ValidationException e) {
            return new ResponseEntity<>(new DeliveryStatusResponseDto("Error: " + e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (DatabaseAccessException e) {
            return new ResponseEntity<>(new DeliveryStatusResponseDto("Error: Database operation failed - " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>(new DeliveryStatusResponseDto("Error: An unexpected error occurred - " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<DeliveryStatusResponseDto> getAllPartners() {
        try {
            List<DeliveryPersonDTO> partners = partnerService.getAllPartners();
            DeliveryStatusResponseDto response = new DeliveryStatusResponseDto("Success: Retrieved all partners.", partners, true, true);
            return ResponseEntity.ok(response);
        } catch (DatabaseAccessException e) {
            return new ResponseEntity<>(new DeliveryStatusResponseDto("Error: Database operation failed - " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>(new DeliveryStatusResponseDto("Error: An unexpected error occurred - " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeliveryStatusResponseDto> getPartnerById(@PathVariable Long id) {
        try {
            DeliveryPersonDTO partner = partnerService.getPartnerById(id);
            DeliveryStatusResponseDto response = new DeliveryStatusResponseDto("Success: Retrieved partner.", partner);
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

    @PutMapping("/{id}")
    public ResponseEntity<DeliveryStatusResponseDto> updatePartner(
            @PathVariable Long id,
            @RequestBody DeliveryPersonDTO request) {
        try {
            DeliveryPersonDTO updated = partnerService.updatePartner(id, request);
            DeliveryStatusResponseDto response = new DeliveryStatusResponseDto("Success: Partner updated.", updated);
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
    public ResponseEntity<DeliveryStatusResponseDto> deletePartner(@PathVariable Long id) {
        try {
            partnerService.deletePartner(id);
            return ResponseEntity.ok(new DeliveryStatusResponseDto("Success: Partner deleted."));
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
