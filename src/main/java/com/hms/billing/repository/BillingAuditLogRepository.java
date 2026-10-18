package com.hms.billing.repository;

import com.hms.billing.entity.BillingAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillingAuditLogRepository extends JpaRepository<BillingAuditLog, Long> {
    Page<BillingAuditLog> findByBillId(Long billId, Pageable pageable);

    @Query("""
           select l from BillingAuditLog l
           where (:billId is null or l.billId = :billId)
             and (:q is null or lower(l.patientName) like lower(concat('%', :q, '%'))
                             or l.mobileNumber like concat('%', :q, '%')
                             or lower(l.action) like lower(concat('%', :q, '%'))
                             or lower(l.actorName) like lower(concat('%', :q, '%'))
                             or lower(l.actorUsername) like lower(concat('%', :q, '%'))
                             or lower(l.details) like lower(concat('%', :q, '%')))
           """)
    Page<BillingAuditLog> search(@Param("billId") Long billId, @Param("q") String q, Pageable pageable);
}