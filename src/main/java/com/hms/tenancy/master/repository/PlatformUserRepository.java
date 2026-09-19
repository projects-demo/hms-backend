package com.hms.tenancy.master.repository;

import com.hms.tenancy.master.entity.PlatformUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlatformUserRepository extends JpaRepository<PlatformUser, Long> {
    Optional<PlatformUser> findByUsernameAndActiveTrue(String username);
}
