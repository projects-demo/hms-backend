package com.hms.security.jwt;

/**
 * What ends up as the Authentication#getPrincipal() for every authenticated
 * request. Controllers/services pull this via
 * (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()
 * - or more conveniently via a @AuthenticationPrincipal argument.
 */
public record AuthenticatedUser(
        Long userId,
        String username,
        String fullName,
        String role,
        String tenantCode,
        String tenantSchema,
        boolean platformUser
) {}
