package com.hms.config;

import com.hms.tenancy.master.entity.Tenant;
import com.hms.tenancy.master.repository.TenantRepository;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/** Applies newly released tenant migrations to schemas that existed before this application version. */
@Slf4j
@Component
public class TenantFlywayInitializer implements SmartInitializingSingleton {

    private final TenantRepository tenantRepository;
    private final HikariDataSource tenantDataSource;
    private final MasterFlywayInitializer masterFlywayInitializer;

    public TenantFlywayInitializer(TenantRepository tenantRepository,
                                   @Qualifier("tenantDataSource") HikariDataSource tenantDataSource,
                                   MasterFlywayInitializer masterFlywayInitializer) {
        this.tenantRepository = tenantRepository;
        this.tenantDataSource = tenantDataSource;
        this.masterFlywayInitializer = masterFlywayInitializer;
    }

    @Override
    public void afterSingletonsInstantiated() {
        // Ensure newly added master migrations are applied before querying tenant registry rows.
        masterFlywayInitializer.afterSingletonsInstantiated();
        for (Tenant tenant : tenantRepository.findAll()) {
            if (tenant.getStatus() == Tenant.Status.PROVISIONING || tenant.getProvisionedAt() == null) continue;
            String schemaName = tenant.getSchemaName();
            if (schemaName == null || !schemaName.matches("[A-Za-z0-9_]+")) {
                throw new IllegalStateException("Invalid tenant schema name for tenant " + tenant.getTenantCode());
            }

            log.info("Applying tenant migrations for {} ({})", tenant.getTenantCode(), schemaName);
            Flyway.configure()
                    .dataSource(tenantDataSource)
                    .schemas(schemaName)
                    .locations("classpath:db/migration/tenant")
                    .baselineOnMigrate(true)
                    .load()
                    .migrate();
        }
    }
}