-- =====================================================================
-- Hospital branding - single-row settings table (id is always 1).
-- A hospital admin can lightly customize their portal: logo, accent
-- color, and an announcement banner / footer line. Deliberately NOT a
-- full re-theme (buttons/badges/etc. keep the core design system) -
-- just the handful of surfaces real HMS products typically let a
-- hospital brand.
-- =====================================================================
CREATE TABLE tenant_branding (
    id                  BIGINT PRIMARY KEY DEFAULT 1,
    logo_data_url       LONGTEXT NULL,
    primary_color       VARCHAR(7) NULL,   -- hex, e.g. #14635B
    accent_color        VARCHAR(7) NULL,
    header_banner_text  VARCHAR(500) NULL,
    footer_text         VARCHAR(500) NULL,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_branding_singleton CHECK (id = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;