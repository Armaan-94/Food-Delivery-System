package com.fooddelivery.orderservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fooddelivery.orderservice.model.FoodOrder;

public record OrderDto(
        Long id,
        Long userId,
        String customerName,
        String orderDetails,
        BigDecimal totalAmount,
        String orderStatus,
        LocalDateTime orderTimestamp) {

    public static OrderDto from(FoodOrder order) {
        return new OrderDto(order.getId(), order.getUserId(), order.getCustomerName(), order.getOrderDetails(),
                order.getTotalAmount(), order.getOrderStatus(), order.getOrderTimestamp());
    }
}
