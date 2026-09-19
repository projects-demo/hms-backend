package com.hms.tenancy.master.service;

import com.hms.tenancy.master.entity.PlatformUser;
import com.hms.tenancy.master.repository.PlatformUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Local-dev convenience only: if hms_master.platform_users is empty, seeds
 * one PLATFORM_ADMIN so you can call POST /api/v1/platform/auth/login and
 * then POST /api/v1/platform/tenants without hand-inserting a row. In a
 * real environment, disable this (hms.platform.seed-admin.enabled=false)
 * and provision admins deliberately.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformAdminSeeder implements ApplicationRunner {

    private final PlatformUserRepository platformUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${hms.platform.seed-admin.enabled:true}")
    private boolean enabled;

    @Value("${hms.platform.seed-admin.username:platformadmin}")
    private String username;

    @Value("${hms.platform.seed-admin.password:ChangeMe123!}")
    private String password;

    @Override
    @Transactional("masterTransactionManager")
    public void run(ApplicationArguments args) {
        if (!enabled || platformUserRepository.count() > 0) {
            return;
        }
        PlatformUser admin = new PlatformUser();
        admin.setUsername(username);
        admin.setEmail(username + "@hms.local");
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setFullName("Platform Administrator");
        admin.setRole(PlatformUser.Role.PLATFORM_ADMIN);
        platformUserRepository.save(admin);
        log.warn("Seeded default platform admin '{}' - CHANGE THIS PASSWORD before any non-local use.", username);
    }
}
