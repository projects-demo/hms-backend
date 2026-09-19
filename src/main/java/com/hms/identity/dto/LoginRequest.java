package com.hms.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "tenantCode (hospital code) is required") String tenantCode,
        @NotBlank String username,
        @NotBlank String password
) {}
