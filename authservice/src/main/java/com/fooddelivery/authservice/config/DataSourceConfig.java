package com.fooddelivery.authservice.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.config")
    public DataSourceProperties configDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "configDataSource")
    public DataSource configDataSource() {
        return configDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean(name = "configJdbcTemplate")
    public JdbcTemplate configJdbcTemplate(@Qualifier("configDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "appDataSource")
    @Primary
    public DataSource appDataSource(@Qualifier("configJdbcTemplate") JdbcTemplate configJdbcTemplate) {
        System.out.println(">>> Manually configuring 'appDataSource' from config DB...");
        
        String url = getConfigValue(configJdbcTemplate, "spring.datasource.url.authservice");
        String username = getConfigValue(configJdbcTemplate, "spring.datasource.username.authservice");
        String password = getConfigValue(configJdbcTemplate, "spring.datasource.password.authservice");
        String driverClassName = getConfigValue(configJdbcTemplate, "spring.datasource.driver-class-name.authservice");

        if (url == null || username == null || password == null || driverClassName == null) {
            throw new IllegalStateException("Failed to load application database properties from config DB for authservice.");
        }

        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();
    }

    @Bean(name = "appJdbcTemplate")
    @Primary
    public JdbcTemplate appJdbcTemplate(@Qualifier("appDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    private String getConfigValue(JdbcTemplate jdbcTemplate, String key) {
        try {
            String sql = "SELECT config_value FROM service_config WHERE config_key = ?";
            return jdbcTemplate.queryForObject(sql, String.class, key);
        } catch (EmptyResultDataAccessException e) {
            System.err.println("WARN: Configuration key not found in database: " + key);
            return null;
        } catch (Exception e) {
            System.err.println("ERROR: Error fetching configuration key from database: " + key);
            throw new RuntimeException("Error fetching configuration key: " + key, e);
        }
    }
}