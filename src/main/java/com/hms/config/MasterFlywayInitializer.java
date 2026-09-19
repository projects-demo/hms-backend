package com.hms.config;

import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * hms_master is migrated once here, at boot. Individual hospital schemas are
 * migrated on-demand by TenantProvisioningService when they're onboarded -
 * see that class for why tenant schemas can't all be migrated up front
 * (they don't exist yet at application startup).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MasterFlywayInitializer implements ApplicationRunner {

    private final HikariDataSource masterDataSource;

    // Explicit constructor (not Lombok @RequiredArgsConstructor) so the
    // @Qualifier is reliably applied - Lombok does not copy Spring's
    // @Qualifier from a field onto a generated constructor parameter.
    public MasterFlywayInitializer(@Qualifier("masterDataSource") HikariDataSource masterDataSource) {
        this.masterDataSource = masterDataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        Flyway.configure()
                .dataSource(masterDataSource)
                .locations("classpath:db/migration/master")
                .baselineOnMigrate(true)
                .load()
                .migrate();
    }
}
