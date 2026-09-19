package com.hms.branding.dto;

import com.hms.branding.entity.TenantBranding;

public record BrandingResponse(
        String logoDataUrl, String primaryColor, String accentColor,
        String headerBannerText, String footerText
) {
    public static BrandingResponse from(TenantBranding b) {
        return new BrandingResponse(b.getLogoDataUrl(), b.getPrimaryColor(), b.getAccentColor(),
                b.getHeaderBannerText(), b.getFooterText());
    }

    public static BrandingResponse empty() {
        return new BrandingResponse(null, null, null, null, null);
    }
}