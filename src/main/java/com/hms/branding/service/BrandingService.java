package com.hms.branding.service;

import com.hms.branding.dto.BrandingRequest;
import com.hms.branding.dto.BrandingResponse;
import com.hms.branding.entity.TenantBranding;
import com.hms.branding.repository.TenantBrandingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional("transactionManager")
public class BrandingService {

    private final TenantBrandingRepository brandingRepository;

    @Transactional(value = "transactionManager", readOnly = true)
    public BrandingResponse get() {
        return brandingRepository.findById(1L)
                .map(BrandingResponse::from)
                .orElseGet(BrandingResponse::empty);
    }

    public BrandingResponse update(BrandingRequest request) {
        TenantBranding branding = brandingRepository.findById(1L).orElseGet(TenantBranding::new);
        branding.setLogoDataUrl(request.logoDataUrl());
        branding.setPrimaryColor(request.primaryColor());
        branding.setAccentColor(request.accentColor());
        branding.setHeaderBannerText(request.headerBannerText());
        branding.setFooterText(request.footerText());
        return BrandingResponse.from(brandingRepository.save(branding));
    }
}