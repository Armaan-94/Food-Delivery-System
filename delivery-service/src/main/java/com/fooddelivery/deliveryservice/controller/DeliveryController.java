package com.fooddelivery.deliveryservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fooddelivery.common.web.ApiResponse;
import com.fooddelivery.deliveryservice.dto.DeliveryDto;
import com.fooddelivery.deliveryservice.dto.DeliveryRequestDto;
import com.fooddelivery.deliveryservice.dto.DeliveryStatusUpdateDto;
import com.fooddelivery.deliveryservice.service.DeliveryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/deliveries")
@PreAuthorize("hasRole('ADMIN')")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DeliveryDto>> createDelivery(@Valid @RequestBody DeliveryRequestDto request) {
        DeliveryDto created = deliveryService.createDelivery(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Delivery created.", created));
    }

    @GetMapping
    public ApiResponse<List<DeliveryDto>> getAllDeliveries() {
        return ApiResponse.ok(deliveryService.getAllDeliveries());
    }

    @GetMapping("/{id}")
    public ApiResponse<DeliveryDto> getDeliveryById(@PathVariable Long id) {
        return ApiResponse.ok(deliveryService.getDeliveryById(id));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<DeliveryDto> updateDeliveryStatus(@PathVariable Long id,
            @Valid @RequestBody DeliveryStatusUpdateDto request) {
        return ApiResponse.ok("Delivery status updated.", deliveryService.updateDeliveryStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteDelivery(@PathVariable Long id) {
        deliveryService.deleteDelivery(id);
        return ApiResponse.message("Delivery deleted.");
    }
}
