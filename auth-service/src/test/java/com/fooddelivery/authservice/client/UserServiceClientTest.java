package com.fooddelivery.authservice.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.fooddelivery.authservice.dto.LoginRequestDto;
import com.fooddelivery.authservice.dto.SignupRequestDto;
import com.fooddelivery.authservice.dto.UserInfoDto;
import com.fooddelivery.common.exception.AuthenticationFailedException;
import com.fooddelivery.common.exception.DuplicateResourceException;
import com.fooddelivery.common.exception.ServiceUnavailableException;
import com.fooddelivery.common.exception.ValidationException;
import com.fooddelivery.common.security.JwtTokenService;
import com.fooddelivery.common.security.Role;

class UserServiceClientTest {

    private static final String USER_JSON =
            "{\"success\":true,\"data\":{\"id\":7,\"name\":\"Asha\",\"email\":\"a@example.com\",\"role\":\"USER\",\"createdAt\":\"2026-01-01T10:00:00\"}}";

    private MockRestServiceServer server;
    private UserServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://user-service");
        server = MockRestServiceServer.bindTo(builder).build();
        JwtTokenService tokens = mock(JwtTokenService.class);
        when(tokens.issueServiceToken(anyString())).thenReturn("service-token");
        client = new UserServiceClient(builder.build(), tokens);
    }

    @Test
    void registerSendsTheRequestWithAServiceTokenAndMapsTheUser() {
        server.expect(requestTo("http://user-service/internal/users/register"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer service-token"))
                .andExpect(jsonPath("$.email").value("a@example.com"))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(USER_JSON));

        UserInfoDto user = client.register(new SignupRequestDto("Asha", "a@example.com", "password-123"));

        assertThat(user.id()).isEqualTo(7L);
        assertThat(user.role()).isEqualTo(Role.USER);
        server.verify();
    }

    @Test
    void verifyMapsTheUser() {
        server.expect(requestTo("http://user-service/internal/users/verify"))
                .andRespond(withSuccess(USER_JSON, MediaType.APPLICATION_JSON));

        assertThat(client.verify(new LoginRequestDto("a@example.com", "pw")).email()).isEqualTo("a@example.com");
    }

    @Test
    void unauthorizedBecomesAuthenticationFailed() {
        server.expect(requestTo("http://user-service/internal/users/verify")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.verify(new LoginRequestDto("a@example.com", "bad")))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void conflictBecomesDuplicate() {
        server.expect(requestTo("http://user-service/internal/users/register")).andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> client.register(new SignupRequestDto("A", "a@example.com", "password-123")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void badRequestBecomesValidation() {
        server.expect(requestTo("http://user-service/internal/users/register")).andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.register(new SignupRequestDto("A", "a@example.com", "password-123")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void serverErrorsBecomeServiceUnavailable() {
        server.expect(requestTo("http://user-service/internal/users/verify")).andRespond(withServerError());

        assertThatThrownBy(() -> client.verify(new LoginRequestDto("a@example.com", "pw")))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void aRejectedServiceTokenIsAnOutageNotABadPassword() {
        server.expect(requestTo("http://user-service/internal/users/verify")).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> client.verify(new LoginRequestDto("a@example.com", "pw")))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void connectionFailuresBecomeServiceUnavailable() {
        server.expect(requestTo("http://user-service/internal/users/verify"))
                .andRespond(request -> { throw new IOException("connection refused"); });

        assertThatThrownBy(() -> client.verify(new LoginRequestDto("a@example.com", "pw")))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("User service is currently unavailable.");
    }
}
