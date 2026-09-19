-- =====================================================================
-- MASTER SCHEMA (hms_master)
-- Holds ONLY platform-level data: the tenant (hospital) registry and
-- platform administrators. No clinical/patient data ever lives here.
-- =====================================================================

CREATE TABLE tenants (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_code         VARCHAR(50)  NOT NULL,                 -- e.g. "apollo-ghaziabad" - used in login & subdomain
    schema_name         VARCHAR(64)  NOT NULL,                 -- physical MySQL schema, e.g. "tenant_apollo_ghaziabad"
    hospital_name       VARCHAR(200) NOT NULL,
    legal_name          VARCHAR(200),
    contact_email       VARCHAR(150) NOT NULL,
    contact_phone       VARCHAR(20),
    address_line1       VARCHAR(200),
    address_line2       VARCHAR(200),
    city                VARCHAR(100),
    state               VARCHAR(100),
    country             VARCHAR(100) DEFAULT 'India',
    postal_code         VARCHAR(20),
    timezone            VARCHAR(50) DEFAULT 'Asia/Kolkata',
    subscription_plan   VARCHAR(30) NOT NULL DEFAULT 'TRIAL',  -- TRIAL, BASIC, PRO, ENTERPRISE
    status              VARCHAR(20) NOT NULL DEFAULT 'PROVISIONING', -- PROVISIONING, ACTIVE, SUSPENDED, DEACTIVATED
    max_users           INT DEFAULT 25,
    provisioned_at      DATETIME NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_tenants_code UNIQUE (tenant_code),
    CONSTRAINT uq_tenants_schema UNIQUE (schema_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_tenants_status ON tenants (status);

-- Platform-level operators (Anthropic/your-company staff who manage onboarding,
-- billing of hospitals themselves, support access) - NOT hospital staff.
CREATE TABLE platform_users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(100) NOT NULL,
    email           VARCHAR(150) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(150) NOT NULL,
    role            VARCHAR(30)  NOT NULL DEFAULT 'PLATFORM_ADMIN', -- PLATFORM_ADMIN, PLATFORM_SUPPORT
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_platform_users_username UNIQUE (username),
    CONSTRAINT uq_platform_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Audit trail of tenant lifecycle events (provisioning, suspension, etc.)
CREATE TABLE tenant_audit_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    event_type      VARCHAR(50) NOT NULL,   -- CREATED, PROVISIONED, SUSPENDED, REACTIVATED, MIGRATION_RUN
    detail          VARCHAR(1000),
    performed_by    VARCHAR(150),
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tenant_audit_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_tenant_audit_tenant ON tenant_audit_log (tenant_id, created_at);
