ALTER TABLE billing_audit_logs
    ADD COLUMN patient_name VARCHAR(150) NULL,
    ADD COLUMN mobile_number VARCHAR(30) NULL;

UPDATE billing_audit_logs l
JOIN bills b ON l.bill_id = b.id
JOIN patients p ON b.patient_id = p.id
SET l.patient_name = CONCAT(p.first_name, ' ', p.last_name),
    l.mobile_number = p.primary_phone
WHERE l.bill_id IS NOT NULL;

