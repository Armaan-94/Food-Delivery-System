package com.fooddelivery.apigateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.sun.net.httpserver.HttpServer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewaySecurityTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-123456";

    /** Stands in for a downstream service: answers 200 and echoes the Authorization header it received. */
    private static final AtomicInteger DOWNSTREAM_CALLS = new AtomicInteger();
    private static final HttpServer DOWNSTREAM = startDownstream();

    @DynamicPropertySource
    static void downstreamPort(DynamicPropertyRegistry registry) {
        registry.add("test.downstream.port", () -> DOWNSTREAM.getAddress().getPort());
    }

    @AfterAll
    static void stopDownstream() {
        DOWNSTREAM.stop(0);
    }

    @TestConfiguration
    static class DownstreamRoute {
        @Bean
        RouteLocator testRoutes(RouteLocatorBuilder builder, @Value("${test.downstream.port}") int port) {
            return builder.routes()
                    .route("test-downstream", r -> r.path("/api/echo/**").uri("http://localhost:" + port))
                    .route("test-internal", r -> r.path("/internal/**").uri("http://localhost:" + port))
                    .build();
        }
    }

    @Autowired
    WebTestClient client;

    @BeforeEach
    void resetCounter() {
        DOWNSTREAM_CALLS.set(0);
    }

    @Test
    void requestsWithoutATokenNeverReachDownstream() {
        client.get().uri("/api/echo/x").exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.success").isEqualTo(false);

        assertThat(DOWNSTREAM_CALLS).hasValue(0);
    }

    @Test
    void invalidTokensAreRejected() {
        client.get().uri("/api/echo/x").header("Authorization", "Bearer garbage").exchange().expectStatus().isUnauthorized();
        // The old HTTP Basic scheme (email:password on every request) is gone.
        client.get().uri("/api/echo/x").header("Authorization", "Basic dXNlcjpwYXNz").exchange().expectStatus().isUnauthorized();
        assertThat(DOWNSTREAM_CALLS).hasValue(0);
    }

    @Test
    void tokensSignedWithAnotherSecretOrExpiredAreRejected() {
        String forged = token("1", "ADMIN", "another-secret-another-secret-123456", 3600);
        String expired = token("1", "USER", SECRET, -600);

        client.get().uri("/api/echo/x").header("Authorization", "Bearer " + forged).exchange().expectStatus().isUnauthorized();
        client.get().uri("/api/echo/x").header("Authorization", "Bearer " + expired).exchange().expectStatus().isUnauthorized();
        assertThat(DOWNSTREAM_CALLS).hasValue(0);
    }

    @Test
    void serviceTokensAreNotAcceptedFromOutside() {
        client.get().uri("/api/echo/x").header("Authorization", "Bearer " + token("auth-service", "SERVICE", SECRET, 120))
                .exchange().expectStatus().isForbidden()
                .expectBody().jsonPath("$.success").isEqualTo(false);
        assertThat(DOWNSTREAM_CALLS).hasValue(0);
    }

    @Test
    void validTokensAreRoutedAndForwardedUnchanged() {
        String token = token("5", "USER", SECRET, 3600);

        client.get().uri("/api/echo/x").header("Authorization", "Bearer " + token).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("Bearer " + token);
        assertThat(DOWNSTREAM_CALLS).hasValue(1);
    }

    @Test
    void internalPathsAreNeverReachableThroughTheGateway() {
        client.post().uri("/internal/users/verify").exchange().expectStatus().isUnauthorized();
        client.post().uri("/internal/users/verify")
                .header("Authorization", "Bearer " + token("1", "ADMIN", SECRET, 3600))
                .exchange().expectStatus().isForbidden();
        assertThat(DOWNSTREAM_CALLS).hasValue(0);
    }

    @Test
    void onlySignupAndLoginAreOpenAndOtherAuthPathsAreNot() {
        // No auth-service instance exists in this test, so an open route answers with a gateway error, never 401/403.
        int signup = client.post().uri("/auth/signup").exchange().returnResult(String.class).getStatus().value();
        int login = client.post().uri("/auth/login").exchange().returnResult(String.class).getStatus().value();
        assertThat(signup).isNotIn(401, 403);
        assertThat(login).isNotIn(401, 403);

        client.get().uri("/auth/anything-else").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void healthIsPublic() {
        client.get().uri("/actuator/health").exchange().expectStatus().isOk();
    }

    @Test
    void forgedIdentityHeadersGainNothing() {
        client.get().uri("/api/echo/x").header("X-User-Role", "ADMIN").header("X-User-Id", "1").exchange()
                .expectStatus().isUnauthorized();
    }

    private static String token(String subject, String role, String secret, long ttlSeconds) {
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(
                new ImmutableSecret<>(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("food-delivery").subject(subject)
                .issuedAt(now.minusSeconds(Math.max(0, -ttlSeconds) + 60))
                .expiresAt(now.plusSeconds(ttlSeconds))
                .claim("role", role).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private static HttpServer startDownstream() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                DOWNSTREAM_CALLS.incrementAndGet();
                String auth = exchange.getRequestHeaders().getFirst("Authorization");
                byte[] body = (auth == null ? "" : auth).getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
