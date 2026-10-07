package com.fooddelivery.apigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Same settings as the common module's JwtProperties; the gateway is reactive so it cannot depend on common. */
@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32, message = "jwt.secret must be at least 32 characters; set the JWT_SECRET environment variable") String secret,
        @DefaultValue("food-delivery") @NotBlank String issuer) {
}
