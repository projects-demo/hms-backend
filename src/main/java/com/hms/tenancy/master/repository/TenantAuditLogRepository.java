package com.hms.tenancy.master.repository;

import com.hms.tenancy.master.entity.TenantAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantAuditLogRepository extends JpaRepository<TenantAuditLog, Long> {
}
