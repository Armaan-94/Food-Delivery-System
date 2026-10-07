package com.fooddelivery.userservice.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import com.fooddelivery.common.exception.DatabaseAccessException;
import com.fooddelivery.common.exception.DuplicateResourceException;
import com.fooddelivery.common.security.Role;
import com.fooddelivery.userservice.model.User;

@Repository
public class UserRepository {

    private static final RowMapper<User> ROW_MAPPER = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(Role.valueOf(rs.getString("role")));
        user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return user;
    };

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert insert;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.insert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("users")
                .usingColumns("name", "email", "password_hash", "role")
                .usingGeneratedKeyColumns("id");
    }

    public List<User> findAll() {
        return run("load users", () -> jdbcTemplate.query("SELECT * FROM users ORDER BY id", ROW_MAPPER));
    }

    public Optional<User> findById(long id) {
        return run("load user " + id,
                () -> jdbcTemplate.query("SELECT * FROM users WHERE id = ?", ROW_MAPPER, id).stream().findFirst());
    }

    public Optional<User> findByEmail(String email) {
        return run("look up user by email",
                () -> jdbcTemplate.query("SELECT * FROM users WHERE email = ?", ROW_MAPPER, email).stream().findFirst());
    }

    /** Inserts the user and returns its generated id. */
    public long insert(User user) {
        return run("create user", () -> {
            Map<String, Object> columns = new HashMap<>();
            columns.put("name", user.getName());
            columns.put("email", user.getEmail());
            columns.put("password_hash", user.getPasswordHash());
            columns.put("role", user.getRole().name());
            return insert.executeAndReturnKey(columns).longValue();
        });
    }

    /** Returns false when no user has that id. */
    public boolean update(User user) {
        return run("update user " + user.getId(), () -> jdbcTemplate.update(
                "UPDATE users SET name = ?, email = ?, password_hash = ?, role = ? WHERE id = ?",
                user.getName(), user.getEmail(), user.getPasswordHash(), user.getRole().name(), user.getId()) > 0);
    }

    /** Returns false when no user has that id. */
    public boolean deleteById(long id) {
        return run("delete user " + id, () -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", id) > 0);
    }

    private <T> T run(String action, Supplier<T> work) {
        try {
            return work.get();
        } catch (DuplicateKeyException e) {
            throw new DuplicateResourceException("Email is already registered.");
        } catch (DataAccessException e) {
            throw new DatabaseAccessException("Failed to " + action, e);
        }
    }
}
