package com.fooddelivery.orderservice.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fooddelivery.common.security.AuthenticatedUser;
import com.fooddelivery.common.web.ApiResponse;
import com.fooddelivery.orderservice.dto.OrderDto;
import com.fooddelivery.orderservice.dto.PageDto;
import com.fooddelivery.orderservice.service.OrderService;

@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<PageDto<OrderDto>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "orderTimestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(orderService.getOrders(page, size, sortBy, direction, AuthenticatedUser.from(jwt)));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderDto> getOrder(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(orderService.getOrder(id, AuthenticatedUser.from(jwt)));
    }
}
