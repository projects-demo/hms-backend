package com.hms.tenancy.multitenant;

import lombok.RequiredArgsConstructor;
import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * All hospital schemas physically live on ONE MySQL server behind ONE
 * HikariCP pool (the "tenant" datasource). To isolate hospital A's data
 * from hospital B's, we don't switch DataSources - we switch the JDBC
 * connection's active schema (`USE tenant_xxx`) on checkout, and reset it
 * on return. This is what makes "schema-per-tenant with a single pool"
 * work without needing N connection pools for N hospitals.
 */
@RequiredArgsConstructor
public class SchemaMultiTenantConnectionProvider extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl<String> {

    private final DataSource tenantDataSource;

    @Override
    protected DataSource selectAnyDataSource() {
        return tenantDataSource;
    }

    @Override
    protected DataSource selectDataSource(String tenantIdentifier) {
        return tenantDataSource;
    }

@Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        Connection connection = getAnyConnection();
        try {
            // setCatalog(), not setSchema(): for MySQL specifically, setCatalog() is the
            // JDBC call that reliably maps to `USE dbname`. setSchema() is spec-compliant
            // JDBC 4.1 but its actual behavior in MySQL Connector/J depends on the driver's
            // databaseTerm setting and is not guaranteed to switch the active database -
            // this was the source of "No database selected" errors during tenant onboarding.
            connection.setCatalog(tenantIdentifier);
        } catch (SQLException e) {
            connection.close();
            throw new SQLException("Could not switch to tenant schema [" + tenantIdentifier + "]. " +
                    "Verify the schema exists (tenant onboarding may not have finished) and the app DB user has access.", e);
        }
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        // Defensive reset so a pooled connection is never returned mid-tenant-context.
        try {
            connection.setCatalog(null);
        } catch (SQLException ignored) {
            // best-effort - some drivers no-op this
        }
        connection.close();
    }
}
