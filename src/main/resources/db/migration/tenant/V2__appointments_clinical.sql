-- =====================================================================
-- V2: Appointments, Slots, OPD Consultations, Prescriptions
-- =====================================================================

CREATE TABLE appointment_slots (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id       BIGINT NOT NULL,
    slot_date       DATE NOT NULL,
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE, BOOKED, BLOCKED
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_slots_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id),
    CONSTRAINT uq_slot_doctor_time UNIQUE (doctor_id, slot_date, start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- The #1 query pattern: "give me doctor X's available slots on date Y" - must be an index seek, not a scan.
CREATE INDEX idx_slots_doctor_date_status ON appointment_slots (doctor_id, slot_date, status);

CREATE TABLE appointments (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_code    VARCHAR(20) NOT NULL,
    patient_id          BIGINT NOT NULL,
    doctor_id           BIGINT NOT NULL,
    slot_id             BIGINT,
    appointment_date    DATE NOT NULL,
    appointment_type    VARCHAR(20) NOT NULL DEFAULT 'NEW',      -- NEW, FOLLOWUP
    status              VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',-- SCHEDULED, CHECKED_IN, IN_CONSULTATION, COMPLETED, CANCELLED, NO_SHOW
    fee_amount          DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    is_free_visit       BOOLEAN NOT NULL DEFAULT FALSE,
    is_paid             BOOLEAN NOT NULL DEFAULT FALSE,
    notes               VARCHAR(500),
    created_by          BIGINT,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_appt_code UNIQUE (appointment_code),
    CONSTRAINT fk_appt_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_appt_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id),
    CONSTRAINT fk_appt_slot FOREIGN KEY (slot_id) REFERENCES appointment_slots(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_appt_doctor_date ON appointments (doctor_id, appointment_date);
CREATE INDEX idx_appt_patient ON appointments (patient_id);
CREATE INDEX idx_appt_status_date ON appointments (status, appointment_date);

CREATE TABLE appointment_payments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id  BIGINT NOT NULL,
    amount          DECIMAL(10,2) NOT NULL,
    payment_mode    VARCHAR(20) NOT NULL,   -- CASH, CARD, UPI, INSURANCE
    status          VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    paid_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    received_by     BIGINT,
    CONSTRAINT fk_apay_appt FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    CONSTRAINT fk_apay_user FOREIGN KEY (received_by) REFERENCES app_users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_apay_appt ON appointment_payments (appointment_id);

CREATE TABLE consultations (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id      BIGINT NOT NULL,
    patient_id          BIGINT NOT NULL,
    doctor_id           BIGINT NOT NULL,
    chief_complaint     VARCHAR(1000),
    bp                  VARCHAR(20),
    pulse               VARCHAR(10),
    temperature          VARCHAR(10),
    weight_kg           DECIMAL(5,2),
    height_cm           DECIMAL(5,2),
    spo2                VARCHAR(10),
    diagnosis           VARCHAR(1000),
    clinical_notes      TEXT,
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT',  -- DRAFT, COMPLETED
    started_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at        DATETIME NULL,
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_consult_appt UNIQUE (appointment_id),
    CONSTRAINT fk_consult_appt FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    CONSTRAINT fk_consult_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_consult_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_consult_patient ON consultations (patient_id);
CREATE INDEX idx_consult_doctor_date ON consultations (doctor_id, started_at);

CREATE TABLE prescriptions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    consultation_id BIGINT NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_presc_consult UNIQUE (consultation_id),
    CONSTRAINT fk_presc_consult FOREIGN KEY (consultation_id) REFERENCES consultations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE prescription_items (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    prescription_id     BIGINT NOT NULL,
    drug_name           VARCHAR(200) NOT NULL,
    dosage              VARCHAR(100),
    frequency           VARCHAR(100),
    duration            VARCHAR(100),
    instructions        VARCHAR(500),
    CONSTRAINT fk_pitem_presc FOREIGN KEY (prescription_id) REFERENCES prescriptions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_pitem_presc ON prescription_items (prescription_id);
