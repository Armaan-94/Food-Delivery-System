package com.fooddelivery.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "auth.login")
public record LoginProperties(
        @DefaultValue("5") int maxFailedAttempts,
        @DefaultValue("15") long lockoutMinutes) {
}
