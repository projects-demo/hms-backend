package com.hms.identity.dto;

import com.hms.identity.entity.AppUser;

import java.time.LocalDateTime;

public record UserResponse(
        Long id, String username, String email, String fullName, String phone,
        String role, boolean active, LocalDateTime lastLoginAt, LocalDateTime createdAt
) {
    public static UserResponse from(AppUser u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getFullName(), u.getPhone(),
                u.getRole().name(), u.isActive(), u.getLastLoginAt(), u.getCreatedAt());
    }
}
