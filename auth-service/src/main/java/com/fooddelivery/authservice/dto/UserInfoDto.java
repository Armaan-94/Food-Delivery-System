package com.fooddelivery.authservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fooddelivery.common.security.Role;

/** The slice of a user that auth-service needs; mirrors user-service's UserDto. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserInfoDto(Long id, String name, String email, Role role) {
}
