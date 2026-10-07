package com.fooddelivery.userservice.service;

import java.util.List;
import java.util.Locale;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fooddelivery.common.exception.AuthenticationFailedException;
import com.fooddelivery.common.exception.DuplicateResourceException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.common.security.AuthenticatedUser;
import com.fooddelivery.common.security.Role;
import com.fooddelivery.userservice.dto.CreateUserRequestDto;
import com.fooddelivery.userservice.dto.RegisterUserRequestDto;
import com.fooddelivery.userservice.dto.UpdateUserRequestDto;
import com.fooddelivery.userservice.dto.UserDto;
import com.fooddelivery.userservice.model.User;
import com.fooddelivery.userservice.repository.UserRepository;

@Service
public class UserService {

    private static final String INVALID_CREDENTIALS = "Invalid email or password.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    /** Compared against when the email is unknown so that timing does not reveal which emails exist. */
    private final String dummyHash;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.dummyHash = passwordEncoder.encode("timing-equalisation-only");
    }

    public UserDto createUser(CreateUserRequestDto request) {
        Role role = request.role() != null ? request.role() : Role.USER;
        return create(request.name(), request.email(), request.password(), role);
    }

    public UserDto register(RegisterUserRequestDto request) {
        return create(request.name(), request.email(), request.password(), Role.USER);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream().map(UserDto::from).toList();
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(long id) {
        return UserDto.from(findOrThrow(id));
    }

    @Transactional
    public UserDto updateUser(long id, UpdateUserRequestDto request, AuthenticatedUser caller) {
        User user = findOrThrow(id);

        if (request.role() != null && request.role() != user.getRole() && !caller.isAdmin()) {
            throw new AccessDeniedException("Only administrators can change roles.");
        }

        String email = normalizeEmail(request.email());
        userRepository.findByEmail(email).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new DuplicateResourceException("Email is already registered.");
            }
        });

        user.setName(request.name().trim());
        user.setEmail(email);
        if (request.password() != null) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }

        if (!userRepository.update(user)) {
            throw notFound(id);
        }
        return UserDto.from(user);
    }

    @Transactional
    public void deleteUser(long id) {
        if (!userRepository.deleteById(id)) {
            throw notFound(id);
        }
    }

    /** Returns the user when the password matches; otherwise fails with the same error for every cause. */
    @Transactional(readOnly = true)
    public UserDto verifyCredentials(String email, String password) {
        User user = userRepository.findByEmail(normalizeEmail(email)).orElse(null);
        if (user == null) {
            passwordEncoder.matches(password, dummyHash);
            throw new AuthenticationFailedException(INVALID_CREDENTIALS);
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthenticationFailedException(INVALID_CREDENTIALS);
        }
        return UserDto.from(user);
    }

    /** Creates an administrator on startup when none exists with that email. Returns true if created. */
    public boolean createAdminIfAbsent(String name, String email, String password) {
        if (userRepository.findByEmail(normalizeEmail(email)).isPresent()) {
            return false;
        }
        create(name, email, password, Role.ADMIN);
        return true;
    }

    private UserDto create(String name, String email, String password, Role role) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new DuplicateResourceException("Email is already registered.");
        }

        User user = new User();
        user.setName(name.trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);

        long id = userRepository.insert(user);
        return UserDto.from(findOrThrow(id));
    }

    private User findOrThrow(long id) {
        return userRepository.findById(id).orElseThrow(() -> notFound(id));
    }

    private static ResourceNotFoundException notFound(long id) {
        return new ResourceNotFoundException("User not found with id: " + id);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
