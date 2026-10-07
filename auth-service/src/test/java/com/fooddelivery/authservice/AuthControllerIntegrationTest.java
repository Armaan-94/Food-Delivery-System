package com.fooddelivery.authservice;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.authservice.client.UserServiceClient;
import com.fooddelivery.authservice.dto.LoginRequestDto;
import com.fooddelivery.authservice.dto.SignupRequestDto;
import com.fooddelivery.authservice.dto.UserInfoDto;
import com.fooddelivery.common.exception.AuthenticationFailedException;
import com.fooddelivery.common.exception.DuplicateResourceException;
import com.fooddelivery.common.exception.ServiceUnavailableException;
import com.fooddelivery.common.security.Role;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JwtDecoder jwtDecoder;
    @Autowired
    ObjectMapper objectMapper;
    @MockitoBean
    UserServiceClient userServiceClient;

    @Test
    void signupCreatesAnAccountThroughUserService() throws Exception {
        when(userServiceClient.register(any(SignupRequestDto.class)))
                .thenReturn(new UserInfoDto(7L, "Asha", "asha@example.com", Role.USER));

        mockMvc.perform(signup("Asha", "asha@example.com", "password-123"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.email").value("asha@example.com"));
    }

    @Test
    void signupInputIsValidatedBeforeUserServiceIsCalled() throws Exception {
        mockMvc.perform(signup("", "not-an-email", "short"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("email")))
                .andExpect(jsonPath("$.message").value(containsString("password")));

        verifyNoInteractions(userServiceClient);
    }

    @Test
    void duplicateSignupIsAConflict() throws Exception {
        when(userServiceClient.register(any())).thenThrow(new DuplicateResourceException("Email is already registered."));

        mockMvc.perform(signup("Asha", "dup@example.com", "password-123"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email is already registered."));
    }

    @Test
    void loginReturnsASignedTokenCarryingTheUsersIdentityAndRole() throws Exception {
        when(userServiceClient.verify(any(LoginRequestDto.class)))
                .thenReturn(new UserInfoDto(9L, "Boss", "boss@example.com", Role.ADMIN));

        String response = mockMvc.perform(login("boss@example.com", "password-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(3600))
                .andExpect(jsonPath("$.data.user.role").value("ADMIN"))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(response).path("data").path("accessToken").asText();
        Jwt jwt = jwtDecoder.decode(token);
        org.assertj.core.api.Assertions.assertThat(jwt.getSubject()).isEqualTo("9");
        org.assertj.core.api.Assertions.assertThat(jwt.<String>getClaim("role")).isEqualTo("ADMIN");
        org.assertj.core.api.Assertions.assertThat(jwt.<String>getClaim("email")).isEqualTo("boss@example.com");
    }

    @Test
    void wrongCredentialsAreUnauthorized() throws Exception {
        when(userServiceClient.verify(any())).thenThrow(new AuthenticationFailedException("Invalid email or password."));

        mockMvc.perform(login("who@example.com", "nope-nope"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    void repeatedFailuresLockTheAccountEvenForTheCorrectPassword() throws Exception {
        when(userServiceClient.verify(any())).thenThrow(new AuthenticationFailedException("Invalid email or password."));

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(login("locked@example.com", "wrong-pass-" + i)).andExpect(status().isUnauthorized());
        }

        doReturn(new UserInfoDto(3L, "L", "locked@example.com", Role.USER)).when(userServiceClient).verify(any());
        mockMvc.perform(login("locked@example.com", "the-right-password"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false));
        verify(userServiceClient, times(3)).verify(any());
    }

    @Test
    void aSuccessfulLoginDoesNotLockOtherAccounts() throws Exception {
        when(userServiceClient.verify(any())).thenThrow(new AuthenticationFailedException("Invalid email or password."));
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(login("victim@example.com", "wrong-pass-" + i));
        }

        doReturn(new UserInfoDto(4L, "O", "other@example.com", Role.USER)).when(userServiceClient).verify(any());
        mockMvc.perform(login("other@example.com", "password-123")).andExpect(status().isOk());
    }

    @Test
    void userServiceOutageIsReportedAsServiceUnavailable() throws Exception {
        when(userServiceClient.verify(any()))
                .thenThrow(new ServiceUnavailableException("User service is currently unavailable.", new RuntimeException("down")));

        mockMvc.perform(login("down@example.com", "password-123"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("User service is currently unavailable."));
    }

    @Test
    void outageDoesNotCountAsAFailedLoginAttempt() throws Exception {
        when(userServiceClient.verify(any()))
                .thenThrow(new ServiceUnavailableException("User service is currently unavailable.", new RuntimeException("down")));
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(login("outage@example.com", "password-123")).andExpect(status().isServiceUnavailable());
        }
        verify(userServiceClient, times(5)).verify(any());
        verify(userServiceClient, never()).register(any());
    }

    @Test
    void nothingElseIsExposed() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isForbidden());
        mockMvc.perform(get("/auth/anything-else")).andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    private RequestBuilder signup(String name, String email, String password) throws Exception {
        return post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SignupRequestDto(name, email, password)));
    }

    private RequestBuilder login(String email, String password) throws Exception {
        return post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequestDto(email, password)));
    }
}
