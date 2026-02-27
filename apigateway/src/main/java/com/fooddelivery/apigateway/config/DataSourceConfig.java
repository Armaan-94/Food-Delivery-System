package com.fooddelivery.apigateway.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
}