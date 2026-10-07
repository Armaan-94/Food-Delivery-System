package com.fooddelivery.orderservice.service;

import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fooddelivery.common.exception.DatabaseAccessException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.common.exception.ValidationException;
import com.fooddelivery.common.security.AuthenticatedUser;
import com.fooddelivery.orderservice.dto.OrderDto;
import com.fooddelivery.orderservice.dto.PageDto;
import com.fooddelivery.orderservice.model.FoodOrder;
import com.fooddelivery.orderservice.repository.FoodOrderRepository;

@Service
@Transactional(readOnly = true)
public class OrderService {

    static final int MAX_PAGE_SIZE = 100;
    static final String DEFAULT_SORT = "orderTimestamp";

    /** Query parameter value -> entity property. Anything else is rejected rather than passed to the database. */
    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "id", "id",
            "customerName", "customerName",
            "totalAmount", "totalAmount",
            "orderStatus", "orderStatus",
            "orderTimestamp", "orderTimestamp");

    private final FoodOrderRepository orderRepository;

    public OrderService(FoodOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /** Administrators see every order; everyone else sees only their own. */
    public PageDto<OrderDto> getOrders(int page, int size, String sortBy, String direction, AuthenticatedUser caller) {
        if (page < 0) {
            throw new ValidationException("Page index must not be less than zero.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ValidationException("Page size must be between 1 and " + MAX_PAGE_SIZE + ".");
        }

        PageRequest pageable = PageRequest.of(page, size, buildSort(sortBy, direction));
        try {
            Page<FoodOrder> orders = caller.isAdmin()
                    ? orderRepository.findAll(pageable)
                    : orderRepository.findByUserId(caller.id(), pageable);
            return PageDto.from(orders, OrderDto::from);
        } catch (DataAccessException e) {
            throw new DatabaseAccessException("Failed to retrieve orders", e);
        }
    }

    /** A missing order and someone else's order look the same to non-administrators. */
    public OrderDto getOrder(long id, AuthenticatedUser caller) {
        FoodOrder order;
        try {
            order = orderRepository.findById(id).orElse(null);
        } catch (DataAccessException e) {
            throw new DatabaseAccessException("Failed to retrieve order " + id, e);
        }
        if (order == null || (!caller.isAdmin() && !order.getUserId().equals(caller.id()))) {
            throw new ResourceNotFoundException("Order not found with id: " + id);
        }
        return OrderDto.from(order);
    }

    private Sort buildSort(String sortBy, String direction) {
        String field = SORTABLE_FIELDS.get(sortBy == null || sortBy.isBlank() ? DEFAULT_SORT : sortBy);
        if (field == null) {
            throw new ValidationException("Cannot sort by '" + sortBy + "'. Allowed: " + String.join(", ", SORTABLE_FIELDS.keySet().stream().sorted().toList()) + ".");
        }

        String dir = direction == null || direction.isBlank() ? "desc" : direction;
        Sort.Direction sortDirection;
        if (dir.equalsIgnoreCase("asc")) {
            sortDirection = Sort.Direction.ASC;
        } else if (dir.equalsIgnoreCase("desc")) {
            sortDirection = Sort.Direction.DESC;
        } else {
            throw new ValidationException("Sort direction must be 'asc' or 'desc'.");
        }

        Sort sort = Sort.by(sortDirection, field);
        // A unique tie-breaker keeps pages stable when many orders share the same sort value.
        return field.equals("id") ? sort : sort.and(Sort.by(Sort.Direction.ASC, "id"));
    }
}
