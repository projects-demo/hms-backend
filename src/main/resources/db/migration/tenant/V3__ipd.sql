-- =====================================================================
-- V3: IPD / Admissions - wards, beds, admissions, round notes
-- =====================================================================

CREATE TABLE wards (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    ward_type       VARCHAR(20) NOT NULL,   -- GENERAL, ICU, PRIVATE, SEMI_PRIVATE, EMERGENCY
    floor           VARCHAR(20),
    total_beds      INT NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_wards_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE beds (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    ward_id         BIGINT NOT NULL,
    bed_number      VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE, OCCUPIED, MAINTENANCE, RESERVED
    daily_rate      DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    version         BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_beds_ward FOREIGN KEY (ward_id) REFERENCES wards(id),
    CONSTRAINT uq_bed_ward_number UNIQUE (ward_id, bed_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bed-availability lookups happen constantly on the IPD dashboard.
CREATE INDEX idx_beds_ward_status ON beds (ward_id, status);
CREATE INDEX idx_beds_status ON beds (status);

CREATE TABLE admissions (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    admission_code          VARCHAR(20) NOT NULL,
    patient_id              BIGINT NOT NULL,
    admitting_doctor_id     BIGINT NOT NULL,
    bed_id                  BIGINT NOT NULL,
    admission_type          VARCHAR(20) NOT NULL DEFAULT 'PLANNED', -- EMERGENCY, PLANNED, TRANSFER
    reason_for_admission    VARCHAR(1000),
    provisional_diagnosis   VARCHAR(1000),
    final_diagnosis         VARCHAR(1000),
    status                  VARCHAR(20) NOT NULL DEFAULT 'ADMITTED', -- ADMITTED, DISCHARGED, TRANSFERRED, DECEASED
    admission_date          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expected_discharge_date DATE,
    discharge_date           DATETIME NULL,
    discharge_summary        TEXT,
    created_by               BIGINT,
    created_at                DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version                   BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_admission_code UNIQUE (admission_code),
    CONSTRAINT fk_adm_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_adm_doctor FOREIGN KEY (admitting_doctor_id) REFERENCES doctors(id),
    CONSTRAINT fk_adm_bed FOREIGN KEY (bed_id) REFERENCES beds(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_adm_patient ON admissions (patient_id);
CREATE INDEX idx_adm_status ON admissions (status);
CREATE INDEX idx_adm_doctor ON admissions (admitting_doctor_id, status);

CREATE TABLE ipd_round_notes (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    admission_id    BIGINT NOT NULL,
    recorded_by     BIGINT,
    recorded_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    bp              VARCHAR(20),
    pulse           VARCHAR(10),
    temperature     VARCHAR(10),
    spo2            VARCHAR(10),
    notes           TEXT,
    CONSTRAINT fk_round_admission FOREIGN KEY (admission_id) REFERENCES admissions(id),
    CONSTRAINT fk_round_user FOREIGN KEY (recorded_by) REFERENCES app_users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_round_admission_time ON ipd_round_notes (admission_id, recorded_at);
