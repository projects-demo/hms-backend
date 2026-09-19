package com.hms.tenancy.multitenant;

/**
 * Holds the resolved tenant's MySQL schema name for the duration of the
 * current request thread. Set by TenantFilter, read by
 * CurrentTenantIdentifierResolverImpl (Hibernate) on every query,
 * cleared in a `finally` block so nothing ever leaks across requests
 * or gets reused from a pooled thread.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_SCHEMA = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setSchema(String schemaName) {
        CURRENT_SCHEMA.set(schemaName);
    }

    public static String getSchema() {
        return CURRENT_SCHEMA.get();
    }

    public static void clear() {
        CURRENT_SCHEMA.remove();
    }
}
