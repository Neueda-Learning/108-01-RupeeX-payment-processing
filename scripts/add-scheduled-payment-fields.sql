-- Migration: Add scheduled-payment fields to payments table
-- Date: 2026-08-06
-- Description: Adds scheduled_at (IST release time) and origin_country
-- (persisted for later fraud re-evaluation) to support scheduled payments.

SET @col_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'payments'
  AND COLUMN_NAME = 'scheduled_at'
);

SET @sql = IF(@col_exists = 0,
  'ALTER TABLE payments
   ADD COLUMN scheduled_at DATETIME NULL DEFAULT NULL,
   ADD COLUMN origin_country VARCHAR(8) NULL DEFAULT NULL',
  'SELECT "Scheduled payment columns already exist"'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Verification query
SELECT 'Payments table structure:' AS info;
DESCRIBE payments;
