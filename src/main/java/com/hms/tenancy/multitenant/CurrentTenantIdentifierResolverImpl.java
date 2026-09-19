package com.hms.tenancy.multitenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Tells Hibernate which schema to use for the current session. Called on
 * every single query. Falls back to a harmless placeholder schema when no
 * tenant is set (e.g. actuator health checks) rather than throwing, so the
 * app doesn't crash on infra probes - actual tenant-scoped repositories
 * will simply return nothing / fail their own guard checks in that case.
 */
@Component
public class CurrentTenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver<String> {

    @Value("${hms.tenancy.default-fallback-schema:hms_master}")
    private String fallbackSchema;

    @Override
    public String resolveCurrentTenantIdentifier() {
        String schema = TenantContext.getSchema();
        return (schema != null && !schema.isBlank()) ? schema : fallbackSchema;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
