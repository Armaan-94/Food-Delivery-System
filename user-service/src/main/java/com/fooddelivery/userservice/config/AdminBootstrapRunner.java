package com.fooddelivery.userservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.fooddelivery.userservice.service.UserService;

/** Creates the first administrator so there is a way to obtain an ADMIN token on a fresh database. */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final BootstrapAdminProperties properties;
    private final UserService userService;

    public AdminBootstrapRunner(BootstrapAdminProperties properties, UserService userService) {
        this.properties = properties;
        this.userService = userService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isConfigured()) {
            log.info("No bootstrap administrator configured (ADMIN_EMAIL / ADMIN_PASSWORD); skipping.");
            return;
        }
        if (properties.password().length() < 8) {
            throw new IllegalStateException("ADMIN_PASSWORD must be at least 8 characters long.");
        }
        String name = properties.name() == null || properties.name().isBlank() ? "Administrator" : properties.name();
        if (userService.createAdminIfAbsent(name, properties.email(), properties.password())) {
            log.info("Created bootstrap administrator account.");
        } else {
            log.info("Bootstrap administrator already exists; nothing to do.");
        }
    }
}
