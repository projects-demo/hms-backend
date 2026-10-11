ALTER TABLE bills
    ADD COLUMN discount_master_id BIGINT NULL,
    ADD COLUMN discount_percentage DECIMAL(5,2) NULL,
    ADD CONSTRAINT fk_bill_discount_master FOREIGN KEY (discount_master_id) REFERENCES discount_master(id);