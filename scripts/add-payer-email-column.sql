-- Migration: Add payer_email column to payments table
-- Date: 2026-08-05
-- Description: Add email field to capture payer email at payment creation
--              This supports the Notification Service Integration Plan

-- Check if column already exists and add only if it doesn't
SET @col_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'payments'
  AND COLUMN_NAME = 'payer_email'
);

SET @sql = IF(@col_exists = 0,
  'ALTER TABLE payments ADD COLUMN payer_email VARCHAR(255) NULL DEFAULT NULL',
  'SELECT "payer_email column already exists"'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Create index if column was just added
SET @idx_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'payments'
  AND INDEX_NAME = 'idx_payer_email'
);

SET @idx_sql = IF(@idx_exists = 0,
  'ALTER TABLE payments ADD INDEX idx_payer_email (payer_email)',
  'SELECT "idx_payer_email index already exists"'
);

PREPARE idx_stmt FROM @idx_sql;
EXECUTE idx_stmt;
DEALLOCATE PREPARE idx_stmt;

-- Verification query
SELECT 'Payment table structure:' AS info;
DESCRIBE payments;



