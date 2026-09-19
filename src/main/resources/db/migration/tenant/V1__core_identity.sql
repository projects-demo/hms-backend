-- =====================================================================
-- TENANT SCHEMA - applied to EVERY hospital schema (tenant_<code>)
-- V1: Identity, RBAC, org structure, patients
-- =====================================================================

CREATE TABLE app_users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(100) NOT NULL,
    email           VARCHAR(150),
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(150) NOT NULL,
    phone           VARCHAR(20),
    role            VARCHAR(30) NOT NULL,   -- HOSPITAL_ADMIN, DOCTOR, NURSE, RECEPTIONIST, BILLING_STAFF, PHARMACIST, LAB_TECH
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    must_reset_password BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at   DATETIME NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version         BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_app_users_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_app_users_role ON app_users (role);
CREATE INDEX idx_app_users_active ON app_users (is_active);

CREATE TABLE password_reset_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    token       VARCHAR(128) NOT NULL,
    expires_at  DATETIME NOT NULL,
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_prt_token UNIQUE (token),
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE departments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_departments_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE specializations (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    department_id   BIGINT NOT NULL,
    name            VARCHAR(120) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_spec_department FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT uq_spec_dept_name UNIQUE (department_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE doctors (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_code             VARCHAR(20) NOT NULL,
    user_id                 BIGINT NOT NULL,
    department_id           BIGINT NOT NULL,
    specialization          VARCHAR(100) NOT NULL,
    license_number          VARCHAR(50) NOT NULL,
    license_issue_date      DATE,
    license_expiry_date     DATE,
    qualifications          VARCHAR(255),
    experience_years        INT NOT NULL DEFAULT 0,
    consultation_fee        DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    max_patients_per_day    INT NOT NULL DEFAULT 20,
    office_room_number      VARCHAR(50),
    availability_status     VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE, ON_LEAVE, INACTIVE
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version                 BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_doctors_code UNIQUE (doctor_code),
    CONSTRAINT uq_doctors_user UNIQUE (user_id),
    CONSTRAINT fk_doctors_user FOREIGN KEY (user_id) REFERENCES app_users(id),
    CONSTRAINT fk_doctors_dept FOREIGN KEY (department_id) REFERENCES departments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_doctors_department ON doctors (department_id);
CREATE INDEX idx_doctors_status ON doctors (availability_status);
-- Supports typeahead "search doctor by name" (joined) and specialization filter
CREATE INDEX idx_doctors_specialization ON doctors (specialization);

CREATE TABLE doctor_availability (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id           BIGINT NOT NULL,
    day_of_week         TINYINT NOT NULL,      -- 1=Mon .. 7=Sun
    start_time          TIME NOT NULL,
    end_time            TIME NOT NULL,
    slot_duration_mins  INT NOT NULL DEFAULT 15,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_avail_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_availability_doctor_day ON doctor_availability (doctor_id, day_of_week);

CREATE TABLE staff_profiles (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    staff_code      VARCHAR(20) NOT NULL,
    user_id         BIGINT NOT NULL,
    staff_type      VARCHAR(30) NOT NULL,   -- NURSE, RECEPTIONIST, BILLING_STAFF, PHARMACIST, LAB_TECH
    department_id   BIGINT,
    shift           VARCHAR(20),            -- MORNING, EVENING, NIGHT
    joining_date    DATE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version         BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_staff_code UNIQUE (staff_code),
    CONSTRAINT uq_staff_user UNIQUE (user_id),
    CONSTRAINT fk_staff_user FOREIGN KEY (user_id) REFERENCES app_users(id),
    CONSTRAINT fk_staff_dept FOREIGN KEY (department_id) REFERENCES departments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_staff_type ON staff_profiles (staff_type);

CREATE TABLE patients (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_code            VARCHAR(20) NOT NULL,
    first_name              VARCHAR(50) NOT NULL,
    last_name               VARCHAR(50) NOT NULL,
    dob                     DATE,
    age_years               SMALLINT,
    gender                  VARCHAR(10) NOT NULL,   -- MALE, FEMALE, OTHER
    primary_phone           VARCHAR(15) NOT NULL,
    secondary_phone         VARCHAR(15),
    email                   VARCHAR(150),
    emergency_name          VARCHAR(100),
    emergency_contact       VARCHAR(15),
    government_id_type      VARCHAR(20),
    government_id_number    VARCHAR(100),
    insurance_provider      VARCHAR(100),
    policy_number           VARCHAR(100),
    group_id                VARCHAR(100),
    street                  VARCHAR(255),
    city                    VARCHAR(100),
    state                   VARCHAR(100),
    country                 VARCHAR(100),
    zip_code                VARCHAR(20),
    photo_url               VARCHAR(500),
    created_by              BIGINT,
    updated_by              BIGINT,
    created_at               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version                  BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_patients_code UNIQUE (patient_code),
    CONSTRAINT fk_patients_created_by FOREIGN KEY (created_by) REFERENCES app_users(id),
    CONSTRAINT fk_patients_updated_by FOREIGN KEY (updated_by) REFERENCES app_users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Critical for perf: patient search/autocomplete is the single most frequent
-- query in any HMS (reception desk types a name or phone on every visit).
CREATE INDEX idx_patients_phone ON patients (primary_phone);
CREATE INDEX idx_patients_name ON patients (last_name, first_name);
CREATE INDEX idx_patients_govid ON patients (government_id_number);
CREATE FULLTEXT INDEX ftx_patients_name ON patients (first_name, last_name);
