-- V12: Billing advisor type, nurse-doctor assignment, doctor leave date-range
-- ============================================================================

-- Item 3: Add advisor columns to bills table
ALTER TABLE bills 
    ADD COLUMN advisor_type VARCHAR(20) DEFAULT 'SELF',
    ADD COLUMN advisor_doctor_id BIGINT DEFAULT NULL,
    ADD COLUMN external_advisor_name VARCHAR(150) DEFAULT NULL;

-- Item 16: Add leave date-range to doctors for temporary deactivation
ALTER TABLE doctors 
    ADD COLUMN leave_start_date DATE DEFAULT NULL,
    ADD COLUMN leave_end_date DATE DEFAULT NULL;

-- Item 19: Nurse-doctor assignment — dedicated_doctor_id NULL means common/floating nurse
ALTER TABLE staff_profiles 
    ADD COLUMN dedicated_doctor_id BIGINT DEFAULT NULL,
    ADD COLUMN nurse_assignment_type VARCHAR(20) DEFAULT 'COMMON';
