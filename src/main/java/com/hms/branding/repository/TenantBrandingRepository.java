package com.hms.branding.repository;

import com.hms.branding.entity.TenantBranding;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantBrandingRepository extends JpaRepository<TenantBranding, Long> {
}