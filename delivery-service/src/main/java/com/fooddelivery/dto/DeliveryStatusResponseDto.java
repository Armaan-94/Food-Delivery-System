package com.fooddelivery.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryStatusResponseDto {

    private String status;
    private DeliveryResponseDTO delivery;
    private List<DeliveryResponseDTO> deliveries;
    private DeliveryPersonDTO partner;
    private List<DeliveryPersonDTO> partners;

    public DeliveryStatusResponseDto(String status) {
        this.status = status;
    }

    public DeliveryStatusResponseDto(String status, DeliveryResponseDTO delivery) {
        this.status = status;
        this.delivery = delivery;
    }

    public DeliveryStatusResponseDto(String status, List<DeliveryResponseDTO> deliveries, boolean isDeliveryList) {
        this.status = status;
        this.deliveries = deliveries;
    }

    public DeliveryStatusResponseDto(String status, DeliveryPersonDTO partner) {
        this.status = status;
        this.partner = partner;
    }

    public DeliveryStatusResponseDto(String status, List<DeliveryPersonDTO> partners, boolean isPartnerList, boolean dummy) {
        this.status = status;
        this.partners = partners;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public DeliveryResponseDTO getDelivery() {
        return delivery;
    }

    public void setDelivery(DeliveryResponseDTO delivery) {
        this.delivery = delivery;
    }

    public List<DeliveryResponseDTO> getDeliveries() {
        return deliveries;
    }

    public void setDeliveries(List<DeliveryResponseDTO> deliveries) {
        this.deliveries = deliveries;
    }

    public DeliveryPersonDTO getPartner() {
        return partner;
    }

    public void setPartner(DeliveryPersonDTO partner) {
        this.partner = partner;
    }

    public List<DeliveryPersonDTO> getPartners() {
        return partners;
    }

    public void setPartners(List<DeliveryPersonDTO> partners) {
        this.partners = partners;
    }
}
