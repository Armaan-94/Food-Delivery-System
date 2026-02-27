package com.fooddelivery.authservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.fooddelivery.authservice.dto.AuthDto;
import com.fooddelivery.authservice.dto.SignupDto;
import com.fooddelivery.authservice.exceptions.AuthValidationException;
import com.fooddelivery.authservice.exceptions.AuthenticationFailedException;
import com.fooddelivery.authservice.exceptions.DatabaseAccessException;
import com.fooddelivery.authservice.repository.AuthRepository;

@Service
public class AuthService {

    private final AuthRepository _AuthRepository;
    private final PasswordEncoder _PasswordEncoder;

    @Autowired
    AuthService(AuthRepository authRepository, PasswordEncoder passwordEncoder) {
        this._AuthRepository = authRepository;
        this._PasswordEncoder = passwordEncoder;
    }

    public void SignUp(SignupDto cred) {
        validateSignupDto(cred);
        cred.set_Password(_PasswordEncoder.encode(cred.get_Password()));

        try {
            _AuthRepository.SignUp(cred);
        } catch (DatabaseAccessException e) {
            if (e.getMessage() != null && e.getMessage().contains("Email already exists")) {
                throw new AuthValidationException("Email already in use: " + cred.get_Email());
            }
            throw e;
        }
    }

    public void Authenticate(AuthDto cred) {
        validateAuthDto(cred);
        String email = cred.get_Email();
        String providedPassword = cred.get_Password();
        String passwordFromDB;

        try {
            passwordFromDB = _AuthRepository.getPasswordFromEmail(email);
        } catch (EmptyResultDataAccessException e) {
            throw new AuthenticationFailedException("Authentication failed: Invalid email or password.");
        }

        if (!_PasswordEncoder.matches(providedPassword, passwordFromDB)) {
            throw new AuthenticationFailedException("Authentication failed: Invalid email or password.");
        }
    }

    private void validateSignupDto(SignupDto dto) {
        if (dto == null) {
            throw new AuthValidationException("Signup request cannot be null.");
        }
        if (dto.get_Name() == null || dto.get_Name().trim().isEmpty()) {
            throw new AuthValidationException("Name cannot be empty.");
        }
        if (dto.get_Email() == null || dto.get_Email().trim().isEmpty() || !dto.get_Email().contains("@")) {
            throw new AuthValidationException("Valid email is required.");
        }
        if (dto.get_Password() == null || dto.get_Password().length() < 6) {
            throw new AuthValidationException("Password must be at least 6 characters long.");
        }
    }

    private void validateAuthDto(AuthDto dto) {
        if (dto == null) {
            throw new AuthValidationException("Authentication request cannot be null.");
        }
        if (dto.get_Email() == null || dto.get_Email().trim().isEmpty()) {
            throw new AuthValidationException("Email cannot be empty.");
        }
        if (dto.get_Password() == null || dto.get_Password().isEmpty()) {
            throw new AuthValidationException("Password cannot be empty.");
        }
    }
}
