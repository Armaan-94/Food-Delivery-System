package com.fooddelivery.orderservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fooddelivery.orderservice.exceptions.DatabaseAccessException;
import com.fooddelivery.orderservice.exceptions.ValidationException;
import com.fooddelivery.orderservice.model.FoodOrder;
import com.fooddelivery.orderservice.repository.FoodOrderRepository;

@Service
public class OrderService {

    @Autowired
    private FoodOrderRepository foodOrderRepository;

    public Page<FoodOrder> getOrders(int page, int size, String sortBy, String direction) {

        if (page < 0) {
            throw new ValidationException("Page index must not be less than zero.");
        }
        if (size < 1) {
            throw new ValidationException("Page size must not be less than one.");
        }
        if (!StringUtils.hasText(sortBy)) {
            sortBy = "orderTimestamp";
        }
        if (!StringUtils.hasText(direction)) {
            direction = "desc";
        }

        Sort sort;
        try {
            Sort.Direction sortDirection = direction.equalsIgnoreCase(Sort.Direction.ASC.name()) ?
                    Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(sortDirection, sortBy);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid sort direction provided: " + direction);
        } catch (Exception e) {
            throw new ValidationException("Invalid sort property provided: " + sortBy);
        }

        Pageable pageable = PageRequest.of(page, size, sort);

        try {
            return foodOrderRepository.findAll(pageable);
        } catch (DataAccessException e) {
            throw new DatabaseAccessException("Failed to retrieve orders from database.", e);
        } catch (Exception e) {
            throw new DatabaseAccessException("An unexpected error occurred while retrieving orders.", e);
        }
    }
}
