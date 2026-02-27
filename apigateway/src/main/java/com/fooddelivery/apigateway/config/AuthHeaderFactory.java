package com.fooddelivery.apigateway.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fooddelivery.apigateway.repository.ConfigurationRepository;

import jakarta.annotation.PostConstruct;

@Component
public class AuthHeaderFactory {

    @Autowired
    private ConfigurationRepository configRepository;

    private String userServiceUsername;
    private String userServicePassword;
    private String deliveryServiceUsername;
    private String deliveryServicePassword;
    private String sharedSecret;

    @PostConstruct
    public void loadCredentialsFromDb() {
        System.out.println(">>> AuthHeaderFactory: Loading credentials from DB...");
        this.userServiceUsername = getConfigOrThrow("user-service.auth.username");
        this.userServicePassword = getConfigOrThrow("user-service.auth.password");
        this.deliveryServiceUsername = getConfigOrThrow("delivery-service.auth.username");
        this.deliveryServicePassword = getConfigOrThrow("delivery-service.auth.password");
        this.sharedSecret = getConfigOrThrow("apigateway.shared.secret");
        System.out.println(">>> AuthHeaderFactory: Successfully loaded credentials.");
    }

    private String getConfigOrThrow(String key) {
        String value = configRepository.getConfigValue(key);
        if (value == null) {
            throw new IllegalStateException("Required config key '" + key + "' not found in database.");
        }
        return value;
    }

    public String BuildAuthHeader(String serviceName) {
        String username = "";
        String password = "";

        if ("delivery-service".equalsIgnoreCase(serviceName)) {
            username = deliveryServiceUsername;
            password = deliveryServicePassword;
        } else if ("user-service".equalsIgnoreCase(serviceName)) {
            username = userServiceUsername;
            password = userServicePassword;
        } else {
            throw new IllegalArgumentException("Unknown service name for auth header: " + serviceName);
        }

        String auth = username + ":" + password;
        return "Basic " + Base64.getEncoder()
                .encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    }

    public String getSharedSecret() {
        return sharedSecret;
    }
}