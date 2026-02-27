package com.fooddelivery.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationRepository {

    private final JdbcTemplate configJdbcTemplate;

    @Autowired
    public ConfigurationRepository(@Qualifier("configJdbcTemplate") JdbcTemplate configJdbcTemplate) {
        this.configJdbcTemplate = configJdbcTemplate;
    }

    public String getConfigValue(String key) {
        try {
            String sql = "SELECT config_value FROM service_config WHERE config_key = ?";
            return configJdbcTemplate.queryForObject(sql, String.class, key);
        } catch (EmptyResultDataAccessException e) {
            System.err.println("WARN: Configuration key not found in database: " + key);
            return null;
        } catch (Exception e) {
            System.err.println("ERROR: Error fetching configuration key from database: " + key);
            throw new RuntimeException("Error fetching configuration key: " + key, e);
        }
    }
}