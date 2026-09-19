package com.hms.identity.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        Long userId,
        String username,
        String fullName,
        String role,
        String tenantCode,
        String hospitalName
) {}
