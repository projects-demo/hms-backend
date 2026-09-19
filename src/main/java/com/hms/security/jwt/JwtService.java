package com.hms.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * Issues and validates JWTs. Every tenant-user token carries the resolved
 * schema name as a claim (`schema`) so that, on every subsequent request,
 * JwtAuthenticationFilter can populate TenantContext WITHOUT hitting the
 * master tenants table again - the tenant lookup only happens once, at
 * login.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final String issuer;
    private final long accessTtlMinutes;
    private final long refreshTtlDays;

    public JwtService(
            @Value("${hms.security.jwt.secret}") String secretBase64,
            @Value("${hms.security.jwt.issuer}") String issuer,
            @Value("${hms.security.jwt.access-token-ttl-minutes}") long accessTtlMinutes,
            @Value("${hms.security.jwt.refresh-token-ttl-days}") long refreshTtlDays) {
        this.signingKey = Keys.hmacShaKeyFor(java.util.Base64.getDecoder().decode(secretBase64));
        this.issuer = issuer;
        this.accessTtlMinutes = accessTtlMinutes;
        this.refreshTtlDays = refreshTtlDays;
    }

    public String generateAccessToken(String subjectUsername, Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(subjectUsername)
                .addClaims(claims)
                .claim("type", "ACCESS")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(accessTtlMinutes * 60)))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String subjectUsername, Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(subjectUsername)
                .addClaims(claims)
                .claim("type", "REFRESH")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(refreshTtlDays * 24 * 3600)))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .requireIssuer(issuer)
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessTtlMinutes() {
        return accessTtlMinutes;
    }
}
