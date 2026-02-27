package com.fooddelivery.orderservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fooddelivery.orderservice.dto.OrderResponseDto;
import com.fooddelivery.orderservice.exceptions.DatabaseAccessException;
import com.fooddelivery.orderservice.exceptions.ValidationException;
import com.fooddelivery.orderservice.model.FoodOrder;
import com.fooddelivery.orderservice.service.OrderService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping
    public ResponseEntity<OrderResponseDto> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "orderTimestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        OrderResponseDto response = new OrderResponseDto();

        try {
            Page<FoodOrder> orderPage = orderService.getOrders(page, size, sortBy, direction);
            response.setStatus("Success: Retrieved orders.");
            response.setOrderPage(orderPage);
            return ResponseEntity.ok(response);

        } catch (ValidationException e) {
            response.setStatus("Error: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (DatabaseAccessException e) {
            response.setStatus("Error: Database operation failed - " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);

        } catch (Exception e) {
            response.setStatus("Error: An unexpected error occurred - " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
