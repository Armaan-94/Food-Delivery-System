package com.fooddelivery.authservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fooddelivery.authservice.dto.AuthDto;
import com.fooddelivery.authservice.dto.SignupDto;
import com.fooddelivery.authservice.dto.AuthResponseDto;
import com.fooddelivery.authservice.service.AuthService;


import com.fooddelivery.authservice.exceptions.AuthValidationException;
import com.fooddelivery.authservice.exceptions.AuthenticationFailedException;
import com.fooddelivery.authservice.exceptions.DatabaseAccessException;


@RequestMapping("/auth")
@RestController
public class AuthController {
    AuthService _AuthService;

    @Autowired
    AuthController(AuthService authService) {
        this._AuthService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponseDto> SignUp(@RequestBody SignupDto cred) {
        AuthResponseDto response = new AuthResponseDto();
        if (cred != null) {
             response.setEmail(cred.get_Email());
        } else {
             response.setEmail("N/A");
        }

        try {
            _AuthService.SignUp(cred); 
            response.setStatus("Success: User registration successful.");
            return ResponseEntity.ok(response); 

        } catch (AuthValidationException e) {
            response.setStatus("Error: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (DatabaseAccessException e) {
             response.setStatus("Error: Database operation failed during signup - " + e.getMessage());
       
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);

        } catch (Exception e) {
            response.setStatus("Error: An unexpected error occurred during signup - " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthResponseDto> Authenticate(@RequestBody AuthDto cred) {
         AuthResponseDto response = new AuthResponseDto();
         if (cred != null) {
             response.setEmail(cred.get_Email());
         } else {
              response.setEmail("N/A");
         }

         try {
             _AuthService.Authenticate(cred);
             response.setStatus("Success: Authentication successful.");
             return ResponseEntity.ok(response);

         } catch (AuthValidationException e) {
            response.setStatus("Error: " + e.getMessage());
            return ResponseEntity.badRequest().body(response); 

         } catch (AuthenticationFailedException e) {
             response.setStatus("Error: " + e.getMessage());
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);

         } catch (DatabaseAccessException e) {
              response.setStatus("Error: Database operation failed during authentication - " + e.getMessage());
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);

         } catch (Exception e) {
             response.setStatus("Error: An unexpected error occurred during authentication - " + e.getMessage());
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
         }
    }
}