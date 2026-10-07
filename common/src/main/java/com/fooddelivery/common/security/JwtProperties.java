package com.fooddelivery.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Validated
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    @NotBlank
    @Size(min = 32, message = "jwt.secret must be at least 32 characters; set the JWT_SECRET environment variable")
    private String secret;

    @NotBlank
    private String issuer = "food-delivery";

    @Positive
    private long accessTokenMinutes = 60;

    @Positive
    private long serviceTokenSeconds = 120;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public long getAccessTokenMinutes() {
        return accessTokenMinutes;
    }

    public void setAccessTokenMinutes(long accessTokenMinutes) {
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public long getServiceTokenSeconds() {
        return serviceTokenSeconds;
    }

    public void setServiceTokenSeconds(long serviceTokenSeconds) {
        this.serviceTokenSeconds = serviceTokenSeconds;
    }
}
