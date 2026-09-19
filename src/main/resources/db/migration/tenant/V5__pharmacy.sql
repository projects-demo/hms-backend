-- =====================================================================
-- V5: Pharmacy / Inventory - drugs, batches, stock ledger, dispensing
-- =====================================================================

CREATE TABLE drugs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    drug_code       VARCHAR(30) NOT NULL,
    name            VARCHAR(200) NOT NULL,
    generic_name    VARCHAR(200),
    manufacturer    VARCHAR(150),
    category        VARCHAR(100),           -- TABLET, SYRUP, INJECTION, etc.
    unit            VARCHAR(20) NOT NULL DEFAULT 'UNIT',
    reorder_level   INT NOT NULL DEFAULT 10,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version         BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_drugs_code UNIQUE (drug_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Autocomplete on drug name is a hot path during prescription/dispense entry.
CREATE INDEX idx_drugs_name ON drugs (name);
CREATE FULLTEXT INDEX ftx_drugs_name ON drugs (name, generic_name);

CREATE TABLE drug_batches (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    drug_id             BIGINT NOT NULL,
    batch_number        VARCHAR(50) NOT NULL,
    expiry_date         DATE NOT NULL,
    quantity_available  INT NOT NULL DEFAULT 0,
    purchase_price      DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    selling_price       DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    received_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_batch_drug FOREIGN KEY (drug_id) REFERENCES drugs(id),
    CONSTRAINT uq_batch_drug_number UNIQUE (drug_id, batch_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- FEFO (first-expiry-first-out) dispensing needs an index ordered by expiry per drug.
CREATE INDEX idx_batches_drug_expiry ON drug_batches (drug_id, expiry_date);
CREATE INDEX idx_batches_expiry ON drug_batches (expiry_date);

CREATE TABLE stock_transactions (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    drug_id             BIGINT NOT NULL,
    batch_id            BIGINT NOT NULL,
    txn_type            VARCHAR(20) NOT NULL,   -- IN, OUT, ADJUSTMENT
    quantity             INT NOT NULL,
    reference_type       VARCHAR(30),            -- PURCHASE, DISPENSE, ADJUSTMENT, RETURN
    reference_id          BIGINT,
    performed_by          BIGINT,
    txn_at                 DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_stxn_drug FOREIGN KEY (drug_id) REFERENCES drugs(id),
    CONSTRAINT fk_stxn_batch FOREIGN KEY (batch_id) REFERENCES drug_batches(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_stxn_drug_date ON stock_transactions (drug_id, txn_at);
CREATE INDEX idx_stxn_batch ON stock_transactions (batch_id);

CREATE TABLE dispenses (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    dispense_code   VARCHAR(20) NOT NULL,
    prescription_id BIGINT NULL,
    patient_id      BIGINT NOT NULL,
    dispensed_by    BIGINT,
    total_amount    DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    dispensed_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_dispense_code UNIQUE (dispense_code),
    CONSTRAINT fk_dispense_presc FOREIGN KEY (prescription_id) REFERENCES prescriptions(id),
    CONSTRAINT fk_dispense_patient FOREIGN KEY (patient_id) REFERENCES patients(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_dispense_patient ON dispenses (patient_id);

CREATE TABLE dispense_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    dispense_id     BIGINT NOT NULL,
    drug_id         BIGINT NOT NULL,
    batch_id        BIGINT NOT NULL,
    quantity        INT NOT NULL,
    unit_price      DECIMAL(10,2) NOT NULL,
    amount          DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_ditem_dispense FOREIGN KEY (dispense_id) REFERENCES dispenses(id) ON DELETE CASCADE,
    CONSTRAINT fk_ditem_drug FOREIGN KEY (drug_id) REFERENCES drugs(id),
    CONSTRAINT fk_ditem_batch FOREIGN KEY (batch_id) REFERENCES drug_batches(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_ditem_dispense ON dispense_items (dispense_id);

-- =====================================================================
-- Shared code-sequence counter used by CodeGeneratorService to mint
-- human-readable codes (PT-000123, DR-0007, APT-20260815-0042, ...)
-- atomically per hospital, without relying on AUTO_INCREMENT gaps.
-- =====================================================================

CREATE TABLE code_sequences (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_type     VARCHAR(50) NOT NULL,
    seq_value       BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_code_seq_type UNIQUE (entity_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;