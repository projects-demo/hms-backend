package com.hms.config;

import com.hms.tenancy.multitenant.CurrentTenantIdentifierResolverImpl;
import com.hms.tenancy.multitenant.SchemaMultiTenantConnectionProvider;
import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * PRIMARY persistence unit - every domain module (identity, patient, doctor,
 * staff, appointment, opd, ipd, billing, pharmacy, pharmacy, branding) lives here.
 * Hibernate is configured with a MultiTenantConnectionProvider + CurrentTenantIdentifierResolver
 * so every query/insert/update automatically runs against whatever schema TenantContext
 * currently holds - repository code never needs to know or care which hospital it's serving.
 *
 * IMPORTANT: this MUST be built via Spring Boot's EntityManagerFactoryBuilder
 * (not a hand-rolled LocalContainerEntityManagerFactoryBean + HibernateJpaVendorAdapter),
 * because the builder is what correctly wires Hibernate's multi-tenancy bootstrap.
 * A manual build was tried once during a Cloud Run migration and silently broke
 * tenant schema switching - every query fell back to the master schema instead.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = {
                "com.hms.common.codeseq",
                "com.hms.identity.repository",
                "com.hms.patient.repository",
                "com.hms.doctor.repository",
                "com.hms.staff.repository",
                "com.hms.appointment.repository",
                "com.hms.opd.repository",
                "com.hms.ipd.repository",
                "com.hms.billing.repository",
                "com.hms.pharmacy.repository",
                "com.hms.branding.repository"
        },
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager"
)
public class TenantPersistenceConfig {

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("tenantDataSource") HikariDataSource tenantDataSource,
            CurrentTenantIdentifierResolverImpl tenantIdentifierResolver) {

        Map<String, Object> props = new HashMap<>();
        props.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER,
                new SchemaMultiTenantConnectionProvider(tenantDataSource));
        props.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantIdentifierResolver);
        props.put(AvailableSettings.HBM2DDL_AUTO, "none");
        props.put(AvailableSettings.JDBC_TIME_ZONE, "UTC");
        props.put(AvailableSettings.STATEMENT_BATCH_SIZE, 30);
        props.put(AvailableSettings.ORDER_INSERTS, true);
        props.put(AvailableSettings.ORDER_UPDATES, true);
        props.put(AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, 50);

        return builder
                .dataSource(tenantDataSource)
                .packages("com.hms.common.codeseq", "com.hms.identity.entity", "com.hms.patient.entity",
                        "com.hms.doctor.entity", "com.hms.staff.entity", "com.hms.appointment.entity",
                        "com.hms.opd.entity", "com.hms.ipd.entity", "com.hms.billing.entity",
                        "com.hms.pharmacy.entity", "com.hms.branding.entity")
                .persistenceUnit("tenant")
                .properties(props)
                .build();
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") LocalContainerEntityManagerFactoryBean emf) {
        return new JpaTransactionManager(emf.getObject());
    }

    // Hibernate's schema-based multi-tenancy still requires *a* DataSource bean
    // for non-tenant-aware bootstrap steps; expose the raw one too.
    @Bean
    public DataSource tenantRawDataSource(@Qualifier("tenantDataSource") HikariDataSource ds) {
        return ds;
    }
}