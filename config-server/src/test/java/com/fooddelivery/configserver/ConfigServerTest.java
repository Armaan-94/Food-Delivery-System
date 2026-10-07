package com.fooddelivery.configserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = { "CONFIG_SERVER_USER=cfg", "CONFIG_SERVER_PASSWORD=cfg-test-password" })
class ConfigServerTest {

    @Autowired
    TestRestTemplate rest;

    @Test
    void servesServiceSpecificAndSharedProperties() {
        ResponseEntity<String> response = rest.withBasicAuth("cfg", "cfg-test-password")
                .getForEntity("/user-service/default", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("user-service.properties")   // per-service file
                .contains("user_db")
                .contains("application.properties")    // shared file
                .contains("jwt.secret");
    }

    @Test
    void secretsAreServedAsPlaceholdersNotValues() {
        String body = rest.withBasicAuth("cfg", "cfg-test-password")
                .getForObject("/user-service/default", String.class);

        assertThat(body).contains("${JWT_SECRET}").contains("${MYSQL_PASSWORD}");
    }

    @Test
    void everyServiceHasAConfigurationFile() {
        for (String service : List.of("service-discovery", "api-gateway", "auth-service", "user-service",
                "delivery-service", "order-service")) {
            ResponseEntity<String> response = rest.withBasicAuth("cfg", "cfg-test-password")
                    .getForEntity("/" + service + "/default", String.class);
            assertThat(response.getBody()).as(service).contains(service + ".properties");
        }
    }

    @Test
    void anonymousAndWrongCredentialsAreRejected() {
        assertThat(rest.getForEntity("/user-service/default", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(rest.withBasicAuth("cfg", "wrong").getForEntity("/user-service/default", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void healthEndpointStaysOpenForProbes() {
        assertThat(rest.getForEntity("/actuator/health", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /** Guards against committing credentials again: every password/secret value must be a ${PLACEHOLDER}. */
    @Test
    void servedConfigurationContainsNoHardcodedSecrets() throws IOException {
        Path repo = Paths.get("src/main/resources/config-repo");
        try (Stream<Path> files = Files.list(repo)) {
            for (Path file : files.toList()) {
                for (String line : Files.readAllLines(file)) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("#") || !trimmed.contains("=")) {
                        continue;
                    }
                    String key = trimmed.substring(0, trimmed.indexOf('=')).trim().toLowerCase();
                    String value = trimmed.substring(trimmed.indexOf('=') + 1).trim();
                    if (key.contains("password") || key.contains("secret")) {
                        assertThat(value).as(file.getFileName() + ": " + key).startsWith("${");
                    }
                }
            }
        }
    }
}
