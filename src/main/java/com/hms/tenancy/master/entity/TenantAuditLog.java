package com.hms.tenancy.master.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "tenant_audit_log")
public class TenantAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(length = 1000)
    private String detail;

    @Column(name = "performed_by", length = 150)
    private String performedBy;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    public TenantAuditLog() {}

    public TenantAuditLog(Long tenantId, String eventType, String detail, String performedBy) {
        this.tenantId = tenantId;
        this.eventType = eventType;
        this.detail = detail;
        this.performedBy = performedBy;
    }
}
