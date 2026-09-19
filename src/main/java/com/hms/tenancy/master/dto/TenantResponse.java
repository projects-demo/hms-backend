package com.hms.tenancy.master.dto;

import com.hms.tenancy.master.entity.Tenant;

import java.time.LocalDateTime;

public record TenantResponse(
        Long id,
        String tenantCode,
        String schemaName,
        String hospitalName,
        String contactEmail,
        String contactPhone,
        String city,
        String state,
        String subscriptionPlan,
        String status,
        LocalDateTime provisionedAt,
        LocalDateTime createdAt
) {
    public static TenantResponse from(Tenant t) {
        return new TenantResponse(t.getId(), t.getTenantCode(), t.getSchemaName(), t.getHospitalName(),
                t.getContactEmail(), t.getContactPhone(), t.getCity(), t.getState(),
                t.getSubscriptionPlan().name(), t.getStatus().name(), t.getProvisionedAt(), t.getCreatedAt());
    }
}
