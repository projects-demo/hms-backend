package com.hms.identity.repository;

import com.hms.identity.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsernameAndActiveTrue(String username);
    List<AppUser> findByEmailIgnoreCaseAndActiveTrue(String email);
    boolean existsByUsername(String username);
}
