package com.fooddelivery.deliveryservice.client;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.common.exception.ServiceUnavailableException;
import com.fooddelivery.common.security.BearerTokenForwardingInterceptor;

class OrderClientTest {

    private MockRestServiceServer server;
    private OrderClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://order-service")
                .requestInterceptor(new BearerTokenForwardingInterceptor());
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OrderClient(builder.build());

        Jwt jwt = new Jwt("caller-token", Instant.now(), Instant.now().plusSeconds(60),
                java.util.Map.of("alg", "HS256"), java.util.Map.of("sub", "1", "role", "ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anExistingOrderPassesAndTheCallersTokenIsForwarded() {
        server.expect(requestTo("http://order-service/api/orders/7"))
                .andExpect(header("Authorization", "Bearer caller-token"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatCode(() -> client.assertOrderExists(7)).doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void aMissingOrderBecomesNotFound() {
        server.expect(requestTo("http://order-service/api/orders/8")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.assertOrderExists(8)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void serverErrorsBecomeServiceUnavailable() {
        server.expect(requestTo("http://order-service/api/orders/9")).andRespond(withServerError());

        assertThatThrownBy(() -> client.assertOrderExists(9)).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void otherClientErrorsAreNotMistakenForAMissingOrder() {
        server.expect(requestTo("http://order-service/api/orders/10")).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> client.assertOrderExists(10)).isInstanceOf(ServiceUnavailableException.class);
    }
}
