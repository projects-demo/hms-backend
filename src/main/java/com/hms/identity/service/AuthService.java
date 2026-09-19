package com.hms.identity.service;

import com.hms.identity.dto.LoginRequest;
import com.hms.identity.dto.LoginResponse;
import com.hms.identity.entity.AppUser;
import com.hms.identity.repository.AppUserRepository;
import com.hms.security.jwt.JwtService;
import com.hms.tenancy.master.entity.Tenant;
import com.hms.tenancy.master.service.TenantLookupService;
import com.hms.tenancy.multitenant.TenantContext;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Login is the one endpoint that runs BEFORE any JWT exists, so it has to
 * resolve the tenant itself (from the hospital code the user types in) and
 * set TenantContext manually - see the class comment on JwtAuthenticationFilter
 * for why every other endpoint gets this done automatically instead.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final TenantLookupService tenantLookupService;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    //@Transactional("transactionManager")
    public LoginResponse login(LoginRequest request) {
        Tenant tenant = tenantLookupService.resolveActiveTenant(request.tenantCode());
        TenantContext.setSchema(tenant.getSchemaName());
        try {
            AppUser user = appUserRepository.findByUsernameAndActiveTrue(request.username())
                    .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

            if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                throw new BadCredentialsException("Invalid username or password");
            }

            user.setLastLoginAt(LocalDateTime.now());
            appUserRepository.save(user);

            Map<String, Object> claims = Map.of(
                    "userId", user.getId(),
                    "fullName", user.getFullName(),
                    "role", user.getRole().name(),
                    "schema", tenant.getSchemaName(),
                    "tenantCode", tenant.getTenantCode(),
                    "platform", false
            );

            String access = jwtService.generateAccessToken(user.getUsername(), claims);
            String refresh = jwtService.generateRefreshToken(user.getUsername(), claims);

            return new LoginResponse(access, refresh, jwtService.getAccessTtlMinutes() * 60,
                    user.getId(), user.getUsername(), user.getFullName(), user.getRole().name(),
                    tenant.getTenantCode(), tenant.getHospitalName());
        } finally {
            TenantContext.clear();
        }
    }

    public LoginResponse refresh(String refreshToken) {
        Claims claims = jwtService.parseClaims(refreshToken);
        if (!"REFRESH".equals(claims.get("type"))) {
            throw new BadCredentialsException("Not a refresh token");
        }
        Map<String, Object> newClaims = Map.of(
                "userId", claims.get("userId", Long.class),
                "fullName", claims.get("fullName", String.class),
                "role", claims.get("role", String.class),
                "schema", claims.get("schema", String.class),
                "tenantCode", claims.get("tenantCode", String.class),
                "platform", false
        );
        String username = claims.getSubject();
        String access = jwtService.generateAccessToken(username, newClaims);
        String newRefresh = jwtService.generateRefreshToken(username, newClaims);
        return new LoginResponse(access, newRefresh, jwtService.getAccessTtlMinutes() * 60,
                claims.get("userId", Long.class), username, claims.get("fullName", String.class),
                claims.get("role", String.class), claims.get("tenantCode", String.class), null);
    }
}
