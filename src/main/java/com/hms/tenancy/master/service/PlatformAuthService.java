package com.hms.tenancy.master.service;

import com.hms.identity.dto.LoginResponse;
import com.hms.security.jwt.JwtService;
import com.hms.tenancy.master.entity.PlatformUser;
import com.hms.tenancy.master.repository.PlatformUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlatformAuthService {

    private final PlatformUserRepository platformUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(String username, String password) {
        PlatformUser user = platformUserRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }
        Map<String, Object> claims = Map.of(
                "userId", user.getId(),
                "fullName", user.getFullName(),
                "role", user.getRole().name(),
                "platform", true
        );
        String access = jwtService.generateAccessToken(user.getUsername(), claims);
        String refresh = jwtService.generateRefreshToken(user.getUsername(), claims);
        return new LoginResponse(access, refresh, jwtService.getAccessTtlMinutes() * 60,
                user.getId(), user.getUsername(), user.getFullName(), user.getRole().name(), null, null);
    }
}
