package com.fooddelivery.common.security;

public enum Role {
    USER,
    ADMIN,
    /** Internal service-to-service calls, for example auth-service calling user-service. */
    SERVICE
}
