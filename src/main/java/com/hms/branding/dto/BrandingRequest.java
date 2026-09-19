package com.hms.branding.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BrandingRequest(
        // Data URL (e.g. "data:image/png;base64,...") - kept under ~1.5MB worth of
        // base64 text at the frontend so this never becomes a giant payload.
        String logoDataUrl,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "primaryColor must be a hex color like #14635B")
        String primaryColor,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "accentColor must be a hex color like #FB6F45")
        String accentColor,
        @Size(max = 500) String headerBannerText,
        @Size(max = 500) String footerText
) {}