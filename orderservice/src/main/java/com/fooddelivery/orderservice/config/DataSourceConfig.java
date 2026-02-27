package com.fooddelivery.orderservice.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import com.fooddelivery.orderservice.repository.ConfigurationRepository;

@Configuration
@EnableJpaRepositories(
    basePackages = "com.fooddelivery.orderservice.repository",
    entityManagerFactoryRef = "appEntityManagerFactory",
    transactionManagerRef = "appTransactionManager",
    excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
        type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
        classes = ConfigurationRepository.class
    )
)
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

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties appDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "appDataSource")
    @Primary
    public DataSource appDataSource() {
        return appDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean(name = "appEntityManagerFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean appEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("appDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("com.fooddelivery.orderservice.model")
                .persistenceUnit("app")
                .build();
    }

    @Bean(name = "appTransactionManager")
    @Primary
    public PlatformTransactionManager appTransactionManager(
            @Qualifier("appEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory.getObject());
    }
}