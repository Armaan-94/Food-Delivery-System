package com.fooddelivery.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fooddelivery.dto.CreateUserRequestDTO;
import com.fooddelivery.dto.UserDTO;
import com.fooddelivery.exceptions.DatabaseAccessException;
import com.fooddelivery.exceptions.DuplicateResourceException;
import com.fooddelivery.exceptions.ResourceNotFoundException;
import com.fooddelivery.exceptions.ValidationException;
import com.fooddelivery.model.User;
import com.fooddelivery.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserDTO createUser(CreateUserRequestDTO requestDTO) {
        if (requestDTO == null) {
            throw new ValidationException("Request body cannot be null.");
        }

        validateUserRequest(requestDTO);

        try {
            User existingUser = userRepository.findByEmail(requestDTO.getEmail());
            if (existingUser != null) {
                throw new DuplicateResourceException("Email already in use: " + requestDTO.getEmail());
            }

            User newUser = new User();
            newUser.setName(requestDTO.getName());
            newUser.setEmail(requestDTO.getEmail());
            newUser.setPassword(requestDTO.getPassword());

            userRepository.save(newUser);

            User savedUser = userRepository.findByEmail(requestDTO.getEmail());
            if (savedUser == null) {
                throw new RuntimeException("Failed to retrieve user after saving.");
            }

            return mapToUserDTO(savedUser);

        } catch (ValidationException | DuplicateResourceException e) {
            throw e;
        } catch (DatabaseAccessException e) {
            throw e;
        } catch (Exception e) {
            throw new DatabaseAccessException("Unexpected error while creating user.", e);
        }
    }

    public List<UserDTO> getAllUsers() {
        try {
            return userRepository.findAll()
                    .stream()
                    .map(this::mapToUserDTO)
                    .collect(Collectors.toList());
        } catch (DatabaseAccessException e) {
            throw e;
        } catch (Exception e) {
            throw new DatabaseAccessException("Failed to fetch user list.", e);
        }
    }

    public UserDTO getUserById(Long id) {
        try {
            User user = userRepository.findById(id);
            return mapToUserDTO(user);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (DatabaseAccessException e) {
            throw e;
        } catch (Exception e) {
            throw new DatabaseAccessException("Unexpected error while retrieving user with id: " + id, e);
        }
    }

    public UserDTO updateUser(Long id, CreateUserRequestDTO requestDTO) {
        if (requestDTO == null) {
            throw new ValidationException("Request body cannot be null.");
        }

        validateUserRequest(requestDTO);

        try {
            User existingUser = userRepository.findById(id);

            User userWithNewEmail = userRepository.findByEmail(requestDTO.getEmail());
            if (userWithNewEmail != null && !Objects.equals(userWithNewEmail.getId(), id)) {
                throw new DuplicateResourceException("Email already in use by another user: " + requestDTO.getEmail());
            }

            existingUser.setName(requestDTO.getName());
            existingUser.setEmail(requestDTO.getEmail());
            existingUser.setPassword(requestDTO.getPassword());

            userRepository.update(existingUser);

            return mapToUserDTO(existingUser);

        } catch (ValidationException | ResourceNotFoundException | DuplicateResourceException e) {
            throw e;
        } catch (DatabaseAccessException e) {
            throw e;
        } catch (Exception e) {
            throw new DatabaseAccessException("Unexpected error while updating user with id: " + id, e);
        }
    }

    public void deleteUser(Long id) {
        try {
            userRepository.deleteById(id);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (DatabaseAccessException e) {
            throw e;
        } catch (Exception e) {
            throw new DatabaseAccessException("Unexpected error while deleting user with id: " + id, e);
        }
    }

    private UserDTO mapToUserDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        return dto;
    }

    private void validateUserRequest(CreateUserRequestDTO dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new ValidationException("User name cannot be empty.");
        }
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            throw new ValidationException("User email cannot be empty.");
        }
        if (dto.getPassword() == null || dto.getPassword().isEmpty()) {
            throw new ValidationException("User password cannot be empty.");
        }
    }
}
