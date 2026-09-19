package com.hms.security.jwt;

import com.hms.tenancy.multitenant.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * The single most important class for data isolation in this system:
 * it decodes the JWT, and for tenant users, sets TenantContext to the
 * schema embedded in the token BEFORE the request reaches any controller
 * or repository. Everything downstream (Hibernate multi-tenancy resolver)
 * trusts that context blindly, so this filter - and only this filter -
 * is where the schema for a request gets decided. Always cleared in
 * `finally` so a thread returned to the pool never carries a stale tenant.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                try {
                    Claims claims = jwtService.parseClaims(token);
                    if (!"ACCESS".equals(claims.get("type"))) {
                        throw new JwtException("Not an access token");
                    }
                    String role = claims.get("role", String.class);
                    boolean platformUser = Boolean.TRUE.equals(claims.get("platform", Boolean.class));
                    String tenantSchema = claims.get("schema", String.class);
                    String tenantCode = claims.get("tenantCode", String.class);
                    Long userId = claims.get("userId", Long.class);
                    String fullName = claims.get("fullName", String.class);

                    if (!platformUser && tenantSchema != null) {
                        TenantContext.setSchema(tenantSchema);
                    }

                    AuthenticatedUser principal = new AuthenticatedUser(
                            userId, claims.getSubject(), fullName, role, tenantCode, tenantSchema, platformUser);

                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                    var authToken = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } catch (JwtException | IllegalArgumentException e) {
                    // Visible in the console so "why am I getting 401?" is a 5-second diagnosis,
                    // not a guessing game - shows the exact reason (expired, malformed, bad signature, ...).
                    log.warn("Rejected JWT on {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
                    SecurityContextHolder.clearContext();
                }
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}