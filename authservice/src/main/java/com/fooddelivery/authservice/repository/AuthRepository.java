package com.fooddelivery.authservice.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fooddelivery.authservice.dto.SignupDto;
import com.fooddelivery.authservice.exceptions.DatabaseAccessException;

@Repository(value = "AuthRepository")
public class AuthRepository {

    JdbcTemplate _JdbcTemplate;

    @Autowired
    AuthRepository(JdbcTemplate jdbcTemplate) {
        this._JdbcTemplate = jdbcTemplate;
    }

    public void SignUp(SignupDto cred) {
        try {
            String query = "insert into users (u_name, u_email, u_password) values (?, ?, ?)";
            int rowsAffected = _JdbcTemplate.update(query, cred.get_Name(), cred.get_Email(), cred.get_Password());
            if (rowsAffected == 0) {
                throw new DatabaseAccessException("User signup failed, no rows affected for email: " + cred.get_Email());
            }
        } catch (DuplicateKeyException ex) {
            throw new DatabaseAccessException("User signup failed: Email already exists - " + cred.get_Email(), ex);
        } catch (Exception ex) {
            System.out.println("Error during user signup: " + ex.getMessage());
            throw new DatabaseAccessException("User signup failed for email: " + cred.get_Email(), ex);
        }
    }

    public String getPasswordFromEmail(String email) {
        try {
            String query = "select u_password from users where u_email = ?";
            String password = _JdbcTemplate.queryForObject(query, String.class, email);
            if (password == null) {
                throw new EmptyResultDataAccessException("No password found for email: " + email, 1);
            }
            return password;
        } catch (EmptyResultDataAccessException ex) {
            throw ex;
        } catch (Exception ex) {
            System.out.println("Error during user authentication lookup: " + ex.getMessage());
            throw new DatabaseAccessException("Failed to retrieve password for email: " + email, ex);
        }
    }
}
