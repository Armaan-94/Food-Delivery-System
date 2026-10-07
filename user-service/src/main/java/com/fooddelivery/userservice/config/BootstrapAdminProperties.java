package com.fooddelivery.userservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Optional first administrator, normally supplied through the ADMIN_EMAIL and ADMIN_PASSWORD variables. */
@ConfigurationProperties(prefix = "app.bootstrap-admin")
public record BootstrapAdminProperties(String name, String email, String password) {

    public boolean isConfigured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }
}
