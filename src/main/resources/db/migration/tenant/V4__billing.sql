-- =====================================================================
-- V4: Billing - charge master, bills, receipts, refunds
-- =====================================================================

CREATE TABLE charge_types (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(50) NOT NULL,      -- CONSULTATION, PHARMACY, LAB, BED_CHARGE, PROCEDURE, MISC
    CONSTRAINT uq_charge_types_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE charge_master (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    charge_type_id  BIGINT NOT NULL,
    code            VARCHAR(30) NOT NULL,
    name            VARCHAR(200) NOT NULL,
    default_price   DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_charge_master_code UNIQUE (code),
    CONSTRAINT fk_cm_type FOREIGN KEY (charge_type_id) REFERENCES charge_types(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_charge_master_type ON charge_master (charge_type_id, is_active);
CREATE FULLTEXT INDEX ftx_charge_master_name ON charge_master (name);

CREATE TABLE discount_master (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    discount_kind   VARCHAR(10) NOT NULL,   -- PERCENT, FLAT
    value           DECIMAL(10,2) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_discount_master_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE bills (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    bill_number         VARCHAR(30) NOT NULL,
    patient_id          BIGINT NOT NULL,
    admission_id        BIGINT NULL,
    appointment_id      BIGINT NULL,
    bill_type           VARCHAR(20) NOT NULL,   -- OPD, IPD, PHARMACY
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT, FINALIZED, CANCELLED
    subtotal_amount     DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    discount_amount     DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    tax_amount          DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_amount        DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    paid_amount         DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    balance_amount      DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    created_by          BIGINT,
    finalized_at        DATETIME NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version              BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_bill_number UNIQUE (bill_number),
    CONSTRAINT fk_bill_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_bill_admission FOREIGN KEY (admission_id) REFERENCES admissions(id),
    CONSTRAINT fk_bill_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_bills_patient ON bills (patient_id);
CREATE INDEX idx_bills_status_date ON bills (status, created_at);
CREATE INDEX idx_bills_admission ON bills (admission_id);

CREATE TABLE bill_items (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    bill_id             BIGINT NOT NULL,
    charge_master_id    BIGINT NULL,
    description         VARCHAR(300) NOT NULL,
    quantity            INT NOT NULL DEFAULT 1,
    unit_price          DECIMAL(10,2) NOT NULL,
    amount              DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_bitem_bill FOREIGN KEY (bill_id) REFERENCES bills(id) ON DELETE CASCADE,
    CONSTRAINT fk_bitem_charge FOREIGN KEY (charge_master_id) REFERENCES charge_master(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_bitem_bill ON bill_items (bill_id);

CREATE TABLE bill_receipts (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    bill_id             BIGINT NOT NULL,
    receipt_number      VARCHAR(30) NOT NULL,
    amount              DECIMAL(12,2) NOT NULL,
    payment_mode        VARCHAR(20) NOT NULL,   -- CASH, CARD, UPI, INSURANCE, CHEQUE
    received_by         BIGINT,
    received_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_receipt_number UNIQUE (receipt_number),
    CONSTRAINT fk_receipt_bill FOREIGN KEY (bill_id) REFERENCES bills(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_receipts_bill ON bill_receipts (bill_id);
CREATE INDEX idx_receipts_date ON bill_receipts (received_at);

CREATE TABLE bill_refunds (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    bill_id         BIGINT NOT NULL,
    amount          DECIMAL(12,2) NOT NULL,
    reason          VARCHAR(500),
    refunded_by     BIGINT,
    refunded_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refund_bill FOREIGN KEY (bill_id) REFERENCES bills(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_refunds_bill ON bill_refunds (bill_id);
