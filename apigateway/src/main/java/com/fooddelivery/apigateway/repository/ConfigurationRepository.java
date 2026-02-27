package com.fooddelivery.apigateway.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ConfigurationRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ConfigurationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String getConfigValue(String key) {
        try {
            String sql = "SELECT config_value FROM service_config WHERE config_key = ?";
            return jdbcTemplate.queryForObject(sql, String.class, key);
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Error fetching configuration key: " + key, e);
        }
    }
}