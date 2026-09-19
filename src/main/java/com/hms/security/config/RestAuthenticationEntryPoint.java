package com.hms.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hms.common.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Without this, Spring Security's default behavior for a missing/invalid/
 * expired token is to return 403 Forbidden - indistinguishable from "you're
 * logged in but not allowed." This makes it return the correct 401
 * Unauthorized instead, with the same JSON error envelope as everything else.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws java.io.IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = new ErrorResponse(false,
                "Missing, invalid, or expired access token", HttpStatus.UNAUTHORIZED.value(),
                request.getRequestURI(), Instant.now(), null);
        objectMapper.writeValue(response.getWriter(), body);
    }
}