package com.fooddelivery.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fooddelivery.dto.CreateUserRequestDTO;
import com.fooddelivery.dto.UserDTO;
import com.fooddelivery.dto.UserResponseDto;
import com.fooddelivery.exceptions.DatabaseAccessException;
import com.fooddelivery.exceptions.DuplicateResourceException;
import com.fooddelivery.exceptions.ResourceNotFoundException;
import com.fooddelivery.exceptions.ValidationException;
import com.fooddelivery.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponseDto> createUser(@RequestBody CreateUserRequestDTO requestDTO) {
        UserResponseDto response = new UserResponseDto();
        try {
            UserDTO createdUser = userService.createUser(requestDTO);
            response.setStatus("Success: User created.");
            response.setUser(createdUser);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (ValidationException e) {
            response.setStatus("Error: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } catch (DuplicateResourceException e) {
            response.setStatus("Error: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        } catch (DatabaseAccessException e) {
            response.setStatus("Error: Database operation failed - " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            response.setStatus("Error: An unexpected error occurred - " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<UserResponseDto> getAllUsers() {
        UserResponseDto response = new UserResponseDto();
        try {
            List<UserDTO> users = userService.getAllUsers();
            response.setStatus("Success: Retrieved all users.");
            response.setUsers(users);
            return ResponseEntity.ok(response);
        } catch (DatabaseAccessException e) {
            response.setStatus("Error: Database operation failed - " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            response.setStatus("Error: An unexpected error occurred - " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        UserResponseDto response = new UserResponseDto();
        try {
            UserDTO user = userService.getUserById(id);
            response.setStatus("Success: Retrieved user.");
            response.setUser(user);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            response.setStatus("Error: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } catch (DatabaseAccessException e) {
            response.setStatus("Error: Database operation failed - " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            response.setStatus("Error: An unexpected error occurred - " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> updateUser(@PathVariable Long id, @RequestBody CreateUserRequestDTO requestDTO) {
        UserResponseDto response = new UserResponseDto();
        try {
            UserDTO updatedUser = userService.updateUser(id, requestDTO);
            response.setStatus("Success: User updated.");
            response.setUser(updatedUser);
            return ResponseEntity.ok(response);
        } catch (ValidationException e) {
            response.setStatus("Error: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } catch (DuplicateResourceException e) {
            response.setStatus("Error: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        } catch (ResourceNotFoundException e) {
            response.setStatus("Error: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } catch (DatabaseAccessException e) {
            response.setStatus("Error: Database operation failed - " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            response.setStatus("Error: An unexpected error occurred - " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<UserResponseDto> deleteUser(@PathVariable Long id) {
        UserResponseDto response = new UserResponseDto();
        try {
            userService.deleteUser(id);
            response.setStatus("Success: User deleted.");
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            response.setStatus("Error: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } catch (DatabaseAccessException e) {
            response.setStatus("Error: Database operation failed - " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            response.setStatus("Error: An unexpected error occurred - " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
