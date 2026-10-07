package com.fooddelivery.deliveryservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fooddelivery.common.web.ApiResponse;
import com.fooddelivery.deliveryservice.dto.PartnerDto;
import com.fooddelivery.deliveryservice.dto.PartnerRequestDto;
import com.fooddelivery.deliveryservice.service.PartnerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/partners")
@PreAuthorize("hasRole('ADMIN')")
public class PartnerController {

    private final PartnerService partnerService;

    public PartnerController(PartnerService partnerService) {
        this.partnerService = partnerService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PartnerDto>> createPartner(@Valid @RequestBody PartnerRequestDto request) {
        PartnerDto created = partnerService.createPartner(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Partner created.", created));
    }

    @GetMapping
    public ApiResponse<List<PartnerDto>> getAllPartners() {
        return ApiResponse.ok(partnerService.getAllPartners());
    }

    @GetMapping("/{id}")
    public ApiResponse<PartnerDto> getPartnerById(@PathVariable Long id) {
        return ApiResponse.ok(partnerService.getPartnerById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<PartnerDto> updatePartner(@PathVariable Long id, @Valid @RequestBody PartnerRequestDto request) {
        return ApiResponse.ok("Partner updated.", partnerService.updatePartner(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePartner(@PathVariable Long id) {
        partnerService.deletePartner(id);
        return ApiResponse.message("Partner deleted.");
    }
}
