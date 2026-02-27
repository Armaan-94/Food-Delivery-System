package com.fooddelivery.orderservice.dto;

import org.springframework.data.domain.Page;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fooddelivery.orderservice.model.FoodOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponseDto {

    private String status;
    private Page<FoodOrder> orderPage;

    public OrderResponseDto() {}

    public OrderResponseDto(String status) {
        this.status = status;
    }

    public OrderResponseDto(String status, Page<FoodOrder> orderPage) {
        this.status = status;
        this.orderPage = orderPage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Page<FoodOrder> getOrderPage() {
        return orderPage;
    }

    public void setOrderPage(Page<FoodOrder> orderPage) {
        this.orderPage = orderPage;
    }
}
