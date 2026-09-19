package com.hms.branding.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Single-row settings entity - every hospital schema has exactly one row,
 * always id=1 (enforced by the DB CHECK constraint). See BrandingService
 * for the get-or-create-default logic.
 */
@Getter
@Setter
@Entity
@Table(name = "tenant_branding")
public class TenantBranding {

    @Id
    private Long id = 1L;

    @Lob
    @Column(name = "logo_data_url", columnDefinition = "LONGTEXT")
    private String logoDataUrl;

    @Column(name = "primary_color", length = 7)
    private String primaryColor;

    @Column(name = "accent_color", length = 7)
    private String accentColor;

    @Column(name = "header_banner_text", length = 500)
    private String headerBannerText;

    @Column(name = "footer_text", length = 500)
    private String footerText;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}