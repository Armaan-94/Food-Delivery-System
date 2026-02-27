package com.fooddelivery.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponseDto {
    private String status;
    private UserDTO user;
    private List<UserDTO> users;

    public UserResponseDto() {}

    public UserResponseDto(String status) {
        this.status = status;
    }

    public UserResponseDto(String status, UserDTO user) {
        this.status = status;
        this.user = user;
    }

    public UserResponseDto(String status, List<UserDTO> users) {
        this.status = status;
        this.users = users;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public List<UserDTO> getUsers() {
        return users;
    }

    public void setUsers(List<UserDTO> users) {
        this.users = users;
    }
}
