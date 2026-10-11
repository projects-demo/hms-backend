CREATE TABLE IF NOT EXISTS billing_audit_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    action          VARCHAR(50) NOT NULL,
    entity_type     VARCHAR(50) NOT NULL,
    entity_id       BIGINT,
    bill_id         BIGINT,
    actor_user_id   BIGINT NOT NULL,
    actor_username  VARCHAR(100) NOT NULL,
    actor_name      VARCHAR(150) NOT NULL,
    actor_role      VARCHAR(30) NOT NULL,
    details         TEXT NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @billing_audit_idx_created_sql = (
    SELECT IF(
        (
            SELECT COUNT(*)
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'billing_audit_logs'
              AND index_name = 'idx_billing_audit_created'
        ) = 0,
        'CREATE INDEX idx_billing_audit_created ON billing_audit_logs (created_at)',
        'SELECT 1'
    )
);
PREPARE billing_audit_idx_created FROM @billing_audit_idx_created_sql;
EXECUTE billing_audit_idx_created;
DEALLOCATE PREPARE billing_audit_idx_created;

SET @billing_audit_idx_bill_sql = (
    SELECT IF(
        (
            SELECT COUNT(*)
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'billing_audit_logs'
              AND index_name = 'idx_billing_audit_bill'
        ) = 0,
        'CREATE INDEX idx_billing_audit_bill ON billing_audit_logs (bill_id)',
        'SELECT 1'
    )
);
PREPARE billing_audit_idx_bill FROM @billing_audit_idx_bill_sql;
EXECUTE billing_audit_idx_bill;
DEALLOCATE PREPARE billing_audit_idx_bill;

SET @billing_audit_idx_actor_sql = (
    SELECT IF(
        (
            SELECT COUNT(*)
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'billing_audit_logs'
              AND index_name = 'idx_billing_audit_actor'
        ) = 0,
        'CREATE INDEX idx_billing_audit_actor ON billing_audit_logs (actor_user_id)',
        'SELECT 1'
    )
);
PREPARE billing_audit_idx_actor FROM @billing_audit_idx_actor_sql;
EXECUTE billing_audit_idx_actor;
DEALLOCATE PREPARE billing_audit_idx_actor;