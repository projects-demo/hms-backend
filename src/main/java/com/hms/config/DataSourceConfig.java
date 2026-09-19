package com.hms.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.master")
    public HikariDataSource masterDataSource() {
        return new HikariDataSource();
    }

    /**
     * Backing pool for ALL tenant schemas. Individual connections get their
     * active schema switched at checkout time by SchemaMultiTenantConnectionProvider -
     * see that class for why one pool safely serves every hospital.
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.tenant")
    public HikariDataSource tenantDataSource() {
        return new HikariDataSource();
    }
}
