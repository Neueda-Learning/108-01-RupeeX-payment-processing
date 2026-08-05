-- Migration: Add currency conversion fields to payments table
-- Date: 2026-08-05
-- Description: Add exchange rate and converted amount fields for multi-currency support

-- Check if columns already exist and add only if they don't
SET @col_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'payments'
  AND COLUMN_NAME = 'source_currency'
);

SET @sql = IF(@col_exists = 0,
  'ALTER TABLE payments
   ADD COLUMN source_currency VARCHAR(3) NULL DEFAULT NULL,
   ADD COLUMN destination_currency VARCHAR(3) NULL DEFAULT NULL,
   ADD COLUMN converted_amount DECIMAL(19,2) NULL DEFAULT NULL,
   ADD COLUMN exchange_rate DECIMAL(19,6) NULL DEFAULT NULL',
  'SELECT "Currency conversion columns already exist"'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Verification query
SELECT 'Payments table structure:' AS info;
DESCRIBE payments;

