package com.hms.config;

import com.hms.tenancy.multitenant.CurrentTenantIdentifierResolverImpl;
import com.hms.tenancy.multitenant.SchemaMultiTenantConnectionProvider;
import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * PRIMARY persistence unit - every domain module (identity, patient, doctor,
 * staff, appointment, opd, ipd, billing, pharmacy) lives here. Hibernate is
 * configured with MULTI_TENANT=SCHEMA so every query/insert/update
 * automatically runs against whatever schema TenantContext currently holds -
 * repository code never needs to know or care which hospital it's serving.
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
            @Qualifier("tenantDataSource") HikariDataSource tenantDataSource,
            CurrentTenantIdentifierResolverImpl tenantIdentifierResolver) {

        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(tenantDataSource);
        factory.setPackagesToScan(
                "com.hms.common.codeseq",
                "com.hms.identity.entity",
                "com.hms.patient.entity",
                "com.hms.doctor.entity",
                "com.hms.staff.entity",
                "com.hms.appointment.entity",
                "com.hms.opd.entity",
                "com.hms.ipd.entity",
                "com.hms.billing.entity",
                "com.hms.pharmacy.entity",
                "com.hms.branding.repository",
                "com.hms.branding.entity"
        );
        factory.setPersistenceUnitName("tenant");
        factory.setPersistenceProviderClass(HibernatePersistenceProvider.class);

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        factory.setJpaVendorAdapter(vendorAdapter);

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
        factory.setJpaPropertyMap(props);

        return factory;
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
