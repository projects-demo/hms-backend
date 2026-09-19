package com.hms.tenancy.master.repository;

import com.hms.tenancy.master.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByTenantCode(String tenantCode);
    boolean existsByTenantCode(String tenantCode);
    boolean existsBySchemaName(String schemaName);
    Page<Tenant> findByStatus(Tenant.Status status, Pageable pageable);
    Page<Tenant> findByHospitalNameContainingIgnoreCase(String q, Pageable pageable);
}
