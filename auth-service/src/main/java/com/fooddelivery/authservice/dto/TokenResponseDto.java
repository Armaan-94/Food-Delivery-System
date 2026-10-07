package com.fooddelivery.authservice.dto;

public record TokenResponseDto(String accessToken, String tokenType, long expiresIn, UserInfoDto user) {

    public static TokenResponseDto bearer(String accessToken, long expiresInSeconds, UserInfoDto user) {
        return new TokenResponseDto(accessToken, "Bearer", expiresInSeconds, user);
    }
}
