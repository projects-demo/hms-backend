package com.hms.tenancy.master.service;

import com.hms.common.exception.TenantResolutionException;
import com.hms.tenancy.master.entity.Tenant;
import com.hms.tenancy.master.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Resolves a hospital's public tenant_code (typed by the user at login,
 * or carried as a subdomain/header) to its physical schema name. Cached
 * because this runs on the hot path of EVERY login and EVERY request that
 * carries a raw tenant code instead of an already-issued JWT.
 */
@Service
@RequiredArgsConstructor
public class TenantLookupService {

    private final TenantRepository tenantRepository;

    @Cacheable(value = "tenantBySchemaCode", key = "#tenantCode")
    public Tenant resolveActiveTenant(String tenantCode) {
        Tenant tenant = tenantRepository.findByTenantCode(tenantCode)
                .orElseThrow(() -> new TenantResolutionException("Unknown hospital code: " + tenantCode));
        if (tenant.getStatus() != Tenant.Status.ACTIVE) {
            throw new TenantResolutionException("This hospital account is " + tenant.getStatus() + " - contact support");
        }
        return tenant;
    }
}
