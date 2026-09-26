package com.hms.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final String issuer;
    private final long accessTtlMinutes;
    private final long refreshTtlDays;

    public JwtService(
            // Added safe inline fallback strings directly to the properties values
            @Value("${hms.security.jwt.secret:U0hBTkdFLVRISVMtU0VDUkVULUFORC1NQUtFLSVELVNUUk9ORy1FTk9VR0gtVE8tTUVFVC0yNTYtQklULTFC}") String secretBase64,
            @Value("${hms.security.jwt.issuer:hms-backend}") String issuer,
            @Value("${hms.security.jwt.access-token-ttl-minutes:30}") String accessTtlMinutesStr,
            @Value("${hms.security.jwt.refresh-token-ttl-days:7}") String refreshTtlDaysStr) {
        
        // 1. Decode securely
        byte[] keyBytes = Decoders.BASE64.decode(secretBase64);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.issuer = issuer;
        
        // 2. Safely parse numbers to prevent numeric injection faults
        this.accessTtlMinutes = Long.parseLong(accessTtlMinutesStr.trim());
        this.refreshTtlDays = Long.parseLong(refreshTtlDaysStr.trim());
    }

    public String generateAccessToken(String subjectUsername, Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(subjectUsername)
                .claims(claims)
                .claim("type", "ACCESS")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTtlMinutes * 60)))
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(String subjectUsername, Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(subjectUsername)
                .claims(claims)
                .claim("type", "REFRESH")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTtlDays * 24 * 3600)))
                .signWith(signingKey)
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
