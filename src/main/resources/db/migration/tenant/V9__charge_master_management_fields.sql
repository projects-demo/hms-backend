ALTER TABLE charge_types
    ADD COLUMN code VARCHAR(50) NULL,
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE charge_types
SET code = CONCAT('TYPE_', id)
WHERE code IS NULL;

ALTER TABLE charge_types
    MODIFY COLUMN code VARCHAR(50) NOT NULL,
    ADD CONSTRAINT uq_charge_types_code UNIQUE (code);

ALTER TABLE charge_master
    ADD COLUMN applicable_doctor_id BIGINT NULL,
    ADD CONSTRAINT fk_charge_master_doctor FOREIGN KEY (applicable_doctor_id) REFERENCES doctors(id);