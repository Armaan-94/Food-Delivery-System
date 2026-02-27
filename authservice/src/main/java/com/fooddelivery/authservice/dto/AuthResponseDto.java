package com.fooddelivery.authservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponseDto {

    @JsonProperty("Status")
    private String status;

    @JsonProperty("Email")
    private String email;

    public AuthResponseDto() {}

    public AuthResponseDto(String status, String email) {
        this.status = status;
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
