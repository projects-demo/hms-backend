-- Explicit free-visit billing flag for reliable analytics reporting
ALTER TABLE bills
    ADD COLUMN is_free_visit BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE bills
SET is_free_visit = TRUE
WHERE lower(comments) LIKE '%free visit%';
