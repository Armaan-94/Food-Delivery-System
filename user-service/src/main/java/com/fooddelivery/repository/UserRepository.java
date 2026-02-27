package com.fooddelivery.repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.fooddelivery.exceptions.DatabaseAccessException;
import com.fooddelivery.exceptions.DuplicateResourceException;
import com.fooddelivery.exceptions.ResourceNotFoundException;
import com.fooddelivery.model.User;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        return user;
    };

    public List<User> findAll() {
        try {
            String sql = "SELECT * FROM users";
            return jdbcTemplate.query(sql, userRowMapper);
        } catch (Exception e) {
             throw new DatabaseAccessException("Failed to retrieve all users", e);
        }
    }

    public User findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, new Object[]{id}, userRowMapper);
            if (user == null) {
                 throw new ResourceNotFoundException("User not found with id: " + id);
            }
            return user;
        } catch (EmptyResultDataAccessException e) {
             throw new ResourceNotFoundException("User not found with id: " + id);
        } catch (Exception e) {
             throw new DatabaseAccessException("Failed to find user by id: " + id, e);
        }
    }

    public User findByEmail(String email) {
         String sql = "SELECT * FROM users WHERE email = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, new Object[]{email}, userRowMapper);
             if (user == null) {
                 return null;
             }
             return user;
        } catch (EmptyResultDataAccessException e) {
             return null;
        } catch (Exception e) {
             throw new DatabaseAccessException("Failed to find user by email: " + email, e);
        }
    }

    public void save(User user) {
        try {
            String sql = "INSERT INTO users (name, email, password) VALUES (?, ?, ?)";
            int rowsAffected = jdbcTemplate.update(sql, user.getName(), user.getEmail(), user.getPassword());
            if (rowsAffected == 0) {
                 throw new DatabaseAccessException("User creation failed, no rows affected.");
            }
        } catch (DuplicateKeyException e) {
             throw new DuplicateResourceException("Email already exists: " + user.getEmail());
        } catch (Exception e) {
             throw new DatabaseAccessException("Failed to save user with email: " + user.getEmail(), e);
        }
    }

    public void update(User user) {
        try {
             String sql = "UPDATE users SET name = ?, email = ?, password = ? WHERE id = ?";
             int rowsAffected = jdbcTemplate.update(sql, user.getName(), user.getEmail(), user.getPassword(), user.getId());
              if (rowsAffected == 0) {
                 throw new ResourceNotFoundException("User update failed, user not found with id: " + user.getId());
             }
        } catch (DuplicateKeyException e) {
             throw new DuplicateResourceException("Email already exists: " + user.getEmail());
        } catch (Exception e) {
             throw new DatabaseAccessException("Failed to update user with id: " + user.getId(), e);
        }
    }

    public void deleteById(Long id) {
       try {
            String sql = "DELETE FROM users WHERE id = ?";
            int rowsAffected = jdbcTemplate.update(sql, id);
             if (rowsAffected == 0) {
                 throw new ResourceNotFoundException("User deletion failed, user not found with id: " + id);
            }
       } catch (Exception e) {
             throw new DatabaseAccessException("Failed to delete user with id: " + id, e);
       }
    }

    public boolean existsById(Long id) {
        try {
             findById(id);
             return true;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }
}