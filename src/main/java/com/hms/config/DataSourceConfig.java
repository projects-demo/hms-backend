package com.hms.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Primary
    @Bean(name = "masterDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.master")
    public HikariDataSource masterDataSource() {
        log.info("[HMS CONFIG] Initializing master pool binding from environment/properties");
        return new HikariDataSource();
    }

    /**
     * Backing pool for ALL tenant schemas. Individual connections get their
     * active schema switched at checkout time by SchemaMultiTenantConnectionProvider.
     */
    @Bean(name = "tenantDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.tenant")
    public HikariDataSource tenantDataSource() {
        log.info("[HMS CONFIG] Initializing tenant template pool configuration");
        return new HikariDataSource();
    }
}
