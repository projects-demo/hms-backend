package com.hms.billing.repository;

import com.hms.billing.entity.BillingAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

public interface BillingAuditLogRepository extends Repository<BillingAuditLog, Long> {
    BillingAuditLog save(BillingAuditLog auditLog);
    Page<BillingAuditLog> findAll(Pageable pageable);
    Page<BillingAuditLog> findByBillId(Long billId, Pageable pageable);
}