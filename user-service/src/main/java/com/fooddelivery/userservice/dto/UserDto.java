package com.fooddelivery.userservice.dto;

import java.time.LocalDateTime;

import com.fooddelivery.common.security.Role;
import com.fooddelivery.userservice.model.User;

/** What the API returns for a user. It never contains the password or its hash. */
public record UserDto(Long id, String name, String email, Role role, LocalDateTime createdAt) {

    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
