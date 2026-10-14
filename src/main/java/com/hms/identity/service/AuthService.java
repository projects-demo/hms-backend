package com.hms.identity.service;

import com.hms.identity.dto.LoginRequest;
import com.hms.identity.dto.LoginResponse;
import com.hms.identity.dto.ForgotPasswordRequest;
import com.hms.identity.dto.ResetPasswordRequest;
import com.hms.identity.entity.AppUser;
import com.hms.identity.entity.PasswordResetToken;
import com.hms.identity.repository.AppUserRepository;
import com.hms.identity.repository.PasswordResetTokenRepository;
import com.hms.security.jwt.JwtService;
import com.hms.tenancy.master.entity.Tenant;
import com.hms.tenancy.master.service.TenantLookupService;
import com.hms.tenancy.multitenant.TenantContext;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Login is the one endpoint that runs BEFORE any JWT exists, so it has to
 * resolve the tenant itself (from the hospital code the user types in) and
 * set TenantContext manually - see the class comment on JwtAuthenticationFilter
 * for why every other endpoint gets this done automatically instead.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final TenantLookupService tenantLookupService;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetEmailService passwordResetEmailService;

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

    /** Always responds successfully to avoid revealing whether an email belongs to an account. */
    public void requestPasswordReset(ForgotPasswordRequest request) {
        Tenant tenant = tenantLookupService.resolveActiveTenant(request.tenantCode().trim());
        TenantContext.setSchema(tenant.getSchemaName());
        try {
            List<AppUser> matchingUsers = appUserRepository.findByEmailIgnoreCaseAndActiveTrue(request.email().trim());
            if (matchingUsers.size() != 1) return;
            AppUser user = matchingUsers.get(0);

            passwordResetTokenRepository.findByUserIdAndUsedFalse(user.getId()).forEach(existing -> existing.setUsed(true));
            passwordResetTokenRepository.saveAll(passwordResetTokenRepository.findByUserIdAndUsedFalse(user.getId()));

            byte[] tokenBytes = new byte[32];
            SECURE_RANDOM.nextBytes(tokenBytes);
            String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUserId(user.getId());
            resetToken.setToken(hashToken(rawToken));
            resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(30));
            passwordResetTokenRepository.save(resetToken);
            try {
                passwordResetEmailService.sendResetLink(user.getEmail(), tenant.getHospitalName(), tenant.getTenantCode(), rawToken);
            } catch (MailException exception) {
                log.warn("Unable to send password reset email for user id {}", user.getId(), exception);
            }
        } finally {
            TenantContext.clear();
        }
    }

    public void resetPassword(ResetPasswordRequest request) {
        Tenant tenant = tenantLookupService.resolveActiveTenant(request.tenantCode().trim());
        TenantContext.setSchema(tenant.getSchemaName());
        try {
            PasswordResetToken resetToken = passwordResetTokenRepository
                    .findByTokenAndUsedFalseAndExpiresAtAfter(hashToken(request.token()), LocalDateTime.now())
                    .orElseThrow(() -> new BadCredentialsException("This password reset link is invalid or expired"));
            AppUser user = appUserRepository.findById(resetToken.getUserId())
                    .filter(AppUser::isActive)
                    .orElseThrow(() -> new BadCredentialsException("This password reset link is invalid or expired"));
            user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
            user.setMustResetPassword(false);
            appUserRepository.save(user);
            passwordResetTokenRepository.findByUserIdAndUsedFalse(user.getId()).forEach(token -> token.setUsed(true));
            passwordResetTokenRepository.saveAll(passwordResetTokenRepository.findByUserIdAndUsedFalse(user.getId()));
            resetToken.setUsed(true);
            passwordResetTokenRepository.save(resetToken);
        } finally {
            TenantContext.clear();
        }
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
