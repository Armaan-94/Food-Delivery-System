package com.fooddelivery.authservice.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fooddelivery.authservice.dto.LoginRequestDto;
import com.fooddelivery.authservice.dto.SignupRequestDto;
import com.fooddelivery.authservice.dto.UserInfoDto;
import com.fooddelivery.common.exception.AuthenticationFailedException;
import com.fooddelivery.common.exception.DuplicateResourceException;
import com.fooddelivery.common.exception.ServiceUnavailableException;
import com.fooddelivery.common.exception.ValidationException;
import com.fooddelivery.common.security.JwtTokenService;
import com.fooddelivery.common.web.ApiResponse;

/** Talks to user-service's internal endpoints using a short-lived SERVICE token. */
@Component
public class UserServiceClient {

    private static final String SERVICE_NAME = "auth-service";
    private static final ParameterizedTypeReference<ApiResponse<UserInfoDto>> USER_RESPONSE =
            new ParameterizedTypeReference<>() { };

    private final RestClient restClient;
    private final JwtTokenService tokenService;

    public UserServiceClient(RestClient userServiceRestClient, JwtTokenService tokenService) {
        this.restClient = userServiceRestClient;
        this.tokenService = tokenService;
    }

    public UserInfoDto register(SignupRequestDto request) {
        return post("/internal/users/register", request);
    }

    public UserInfoDto verify(LoginRequestDto request) {
        return post("/internal/users/verify", request);
    }

    private UserInfoDto post(String path, Object body) {
        try {
            ApiResponse<UserInfoDto> response = restClient.post()
                    .uri(path)
                    .headers(headers -> headers.setBearerAuth(tokenService.issueServiceToken(SERVICE_NAME)))
                    .body(body)
                    .retrieve()
                    .body(USER_RESPONSE);
            if (response == null || response.data() == null) {
                throw new ServiceUnavailableException("User service returned an empty response.", null);
            }
            return response.data();
        } catch (RestClientResponseException e) {
            throw translate(e);
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("User service is currently unavailable.", e);
        }
    }

    private RuntimeException translate(RestClientResponseException e) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        if (status == HttpStatus.UNAUTHORIZED) {
            return new AuthenticationFailedException("Invalid email or password.");
        }
        if (status == HttpStatus.CONFLICT) {
            return new DuplicateResourceException("Email is already registered.");
        }
        if (status == HttpStatus.BAD_REQUEST) {
            return new ValidationException("The request was rejected as invalid.");
        }
        return new ServiceUnavailableException("User service is currently unavailable.", e);
    }
}
