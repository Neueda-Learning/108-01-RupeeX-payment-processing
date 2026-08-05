-- Migration: Add payer_email column to payments table
-- Date: 2026-08-05
-- Description: Add email field to capture payer email at payment creation
--              This supports the Notification Service Integration Plan

ALTER TABLE payments ADD COLUMN payer_email VARCHAR(255) NULL DEFAULT NULL;

-- Create index for optional performance optimization if queries filter by payer_email
ALTER TABLE payments ADD INDEX idx_payer_email (payer_email);

-- Verify the column was added
DESCRIBE payments;

