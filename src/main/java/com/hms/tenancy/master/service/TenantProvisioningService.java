package com.hms.tenancy.master.service;

import com.hms.common.exception.DuplicateResourceException;
import com.hms.tenancy.master.dto.OnboardTenantRequest;
import com.hms.tenancy.master.entity.Tenant;
import com.hms.tenancy.master.entity.TenantAuditLog;
import com.hms.tenancy.master.repository.TenantAuditLogRepository;
import com.hms.tenancy.master.repository.TenantRepository;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;

/**
 * Onboards a new hospital: creates its dedicated MySQL schema, runs the
 * tenant Flyway migration set against it, seeds a HOSPITAL_ADMIN login, and
 * flips the tenant row to ACTIVE. This is the ONLY place in the codebase
 * that creates a physical schema - everything else just consumes
 * TenantContext.
 */
@Slf4j
@Service
public class TenantProvisioningService {

    private final TenantRepository tenantRepository;
    private final TenantAuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final HikariDataSource tenantDataSource;

    // Explicit constructor (not Lombok @RequiredArgsConstructor) so the
    // @Qualifier on tenantDataSource is reliably applied - Lombok does not
    // copy Spring's @Qualifier from a field onto a generated constructor parameter.
    public TenantProvisioningService(TenantRepository tenantRepository,
                                      TenantAuditLogRepository auditLogRepository,
                                      PasswordEncoder passwordEncoder,
                                      @Qualifier("tenantDataSource") HikariDataSource tenantDataSource) {
        this.tenantRepository = tenantRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantDataSource = tenantDataSource;
    }

    @Value("${hms.tenancy.schema-prefix:tenant_}")
    private String schemaPrefix;

    public Tenant onboard(OnboardTenantRequest request) {
        String normalizedCode = normalize(request.tenantCode());
        if (tenantRepository.existsByTenantCode(normalizedCode)) {
            throw new DuplicateResourceException("A hospital with tenant code '" + normalizedCode + "' already exists");
        }
        String schemaName = schemaPrefix + normalizedCode.replace('-', '_');
        if (tenantRepository.existsBySchemaName(schemaName)) {
            throw new DuplicateResourceException("Schema name collision for '" + schemaName + "' - choose a different tenant code");
        }

        Tenant tenant = new Tenant();
        tenant.setTenantCode(normalizedCode);
        tenant.setSchemaName(schemaName);
        tenant.setHospitalName(request.hospitalName());
        tenant.setLegalName(request.legalName());
        tenant.setContactEmail(request.contactEmail());
        tenant.setContactPhone(request.contactPhone());
        tenant.setAddressLine1(request.addressLine1());
        tenant.setCity(request.city());
        tenant.setState(request.state());
        tenant.setPostalCode(request.postalCode());
        tenant.setStatus(Tenant.Status.PROVISIONING);
        tenant = tenantRepository.save(tenant);
        auditLogRepository.save(new TenantAuditLog(tenant.getId(), "CREATED", "Tenant row created", "system"));

        try {
            createSchema(schemaName);
            runTenantMigrations(schemaName);
            seedHospitalAdmin(schemaName, request);

            tenant.setStatus(Tenant.Status.ACTIVE);
            tenant.setProvisionedAt(LocalDateTime.now());
            tenant = tenantRepository.save(tenant);
            auditLogRepository.save(new TenantAuditLog(tenant.getId(), "PROVISIONED", "Schema created, migrated, admin seeded", "system"));
        } catch (Exception e) {
            log.error("Provisioning failed for tenant {}", normalizedCode, e);
            tenant.setStatus(Tenant.Status.DEACTIVATED);
            tenantRepository.save(tenant);
            auditLogRepository.save(new TenantAuditLog(tenant.getId(), "PROVISIONING_FAILED", e.getMessage(), "system"));
            throw new IllegalStateException("Tenant provisioning failed: " + e.getMessage(), e);
        }

        return tenant;
    }

    private void createSchema(String schemaName) throws Exception {
        try (Connection conn = tenantDataSource.getConnection(); Statement stmt = conn.createStatement()) {
            // Schema name is derived from a normalized/validated tenant code (see normalize()),
            // never taken raw from the request - safe to interpolate into DDL.
            stmt.executeUpdate("CREATE SCHEMA IF NOT EXISTS `" + schemaName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
    }

    private void runTenantMigrations(String schemaName) {
        Flyway flyway = Flyway.configure()
                .dataSource(tenantDataSource)
                .schemas(schemaName)
                .locations("classpath:db/migration/tenant")
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
    }

    private void seedHospitalAdmin(String schemaName, OnboardTenantRequest request) throws Exception {
        String adminUsername = request.adminUsername();
        String hash = passwordEncoder.encode(request.adminPassword());
        try (Connection conn = tenantDataSource.getConnection()) {
            // setCatalog(), not setSchema() - see SchemaMultiTenantConnectionProvider for why.
            conn.setCatalog(schemaName);
            try (var ps = conn.prepareStatement(
                    "INSERT INTO app_users (username, email, password_hash, full_name, phone, role, is_active) " +
                            "VALUES (?, ?, ?, ?, ?, 'HOSPITAL_ADMIN', TRUE)")) {
                ps.setString(1, adminUsername);
                ps.setString(2, request.contactEmail());
                ps.setString(3, hash);
                ps.setString(4, request.adminFullName());
                ps.setString(5, request.contactPhone());
                ps.executeUpdate();
            }
        }
    }

    /** Lowercase, alphanumeric+hyphen only, so it's safe to fold into a schema name. */
    private String normalize(String rawCode) {
        String cleaned = rawCode.trim().toLowerCase().replaceAll("[^a-z0-9-]", "-").replaceAll("-+", "-");
        if (cleaned.isBlank() || cleaned.length() > 40) {
            throw new IllegalArgumentException("tenantCode must be 1-40 chars of letters/digits/hyphens");
        }
        return cleaned;
    }
}
