ALTER TABLE orders_master
    ADD COLUMN financial_audit_status VARCHAR(40);

UPDATE orders_master
SET financial_audit_status = CASE
    WHEN reconciled = TRUE THEN 'RECONCILED'
    ELSE 'PENDING_SETTLEMENT'
END
WHERE financial_audit_status IS NULL;

ALTER TABLE orders_master
    ALTER COLUMN financial_audit_status SET NOT NULL;

CREATE INDEX idx_orders_master_financial_audit_status
    ON orders_master (financial_audit_status);
