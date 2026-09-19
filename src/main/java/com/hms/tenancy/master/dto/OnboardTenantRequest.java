package com.hms.tenancy.master.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OnboardTenantRequest(
        @NotBlank @Size(max = 40) String tenantCode,
        @NotBlank @Size(max = 200) String hospitalName,
        @Size(max = 200) String legalName,
        @NotBlank @Email String contactEmail,
        String contactPhone,
        String addressLine1,
        String city,
        String state,
        String postalCode,
        @NotBlank String adminUsername,
        @NotBlank String adminFullName,
        @NotBlank @Size(min = 8, message = "Admin password must be at least 8 characters") String adminPassword
) {}
