-- RupeeX seed data (clean & idempotent)
-- Fully resets all tables to seed state - removes ALL user-created data

START TRANSACTION;

-- ============================================================================
-- STEP 1: DISABLE FOREIGN KEY CHECKS FOR CLEAN TRUNCATION
-- ============================================================================
SET FOREIGN_KEY_CHECKS=0;

-- ============================================================================
-- STEP 2: TRUNCATE ALL TABLES (Removes all data, keeps structure)
-- ============================================================================
TRUNCATE TABLE risk_scores;
TRUNCATE TABLE fraud_results;
TRUNCATE TABLE dead_letter_queue;
TRUNCATE TABLE processing_queue;
TRUNCATE TABLE payment_history;
TRUNCATE TABLE system_events;
TRUNCATE TABLE notifications;
TRUNCATE TABLE email_notification_log;
TRUNCATE TABLE payments;
TRUNCATE TABLE fraud_rules;
TRUNCATE TABLE audit_logs;
TRUNCATE TABLE accounts;
TRUNCATE TABLE consents;
TRUNCATE TABLE customers;
TRUNCATE TABLE users;

-- ============================================================================
-- STEP 3: RE-ENABLE FOREIGN KEY CHECKS
-- ============================================================================
SET FOREIGN_KEY_CHECKS=1;

-- Seed accounts (member accounts + admin platform account)
INSERT INTO accounts (account_number, account_holder, account_type, currency, country_code, balance, status, email, created_at, updated_at)
VALUES
  ('ACC-10001', 'Aarav Mehta',        'SAVINGS',  'INR', 'IN', 125000.00, 'ACTIVE', 'aarav.mehta@rupeex.seedaccount', NOW(), NOW()),
  ('ACC-10002', 'Priya Sharma',       'CURRENT',  'INR', 'IN', 340000.00, 'ACTIVE', 'priya.sharma@rupeex.seedaccount', NOW(), NOW()),
  ('ACC-10003', 'Neo Retail Pvt Ltd', 'CURRENT',  'INR', 'IN', 870000.00, 'ACTIVE', 'neo.retail@rupeex.seedaccount', NOW(), NOW()),
  ('ACC-10004', 'Zen Imports LLC',    'CURRENT',  'USD', 'US',  48000.00, 'ACTIVE', 'zen.imports@rupeex.seedaccount', NOW(), NOW()),
  ('ACC-10005', 'Lina Das',           'SAVINGS',  'INR', 'IN',  62500.00, 'ACTIVE', 'lina.das@rupeex.seedaccount', NOW(), NOW()),
  ('ACC-10006', 'Atlas Logistics',    'CURRENT',  'INR', 'IN', 215000.00, 'ACTIVE', 'atlas.logistics@rupeex.seedaccount', NOW(), NOW()),
  ('ACC-ADMIN-001', 'Platform Admin', 'CURRENT',  'INR', 'IN',       0.00, 'ACTIVE', 'admin@rupeex.seedaccount', NOW(), NOW());

-- ============================================================================
-- SEED CUSTOMERS (onboarding service) — one per account + one ADMIN user
-- Fixed UUIDs allow idempotent re-seeding.
-- Status APPROVED so they are active members.
-- ============================================================================
INSERT INTO customers (id, full_name, email, phone, dob, status, account_number, account_type, currency, country_code, role, created_at, updated_at)
VALUES
  (UUID_TO_BIN('a1000001-1001-1001-1001-100000000001'), 'Aarav Mehta',        'aarav.mehta@rupeex.seed',     '+91-9100010001', '1990-04-15', 'APPROVED', 'ACC-10001',    'SAVINGS', 'INR', 'IN', 'MEMBER', NOW(), NOW()),
  (UUID_TO_BIN('a1000002-2002-2002-2002-200000000002'), 'Priya Sharma',       'priya.sharma@rupeex.seed',    '+91-9100020002', '1988-07-22', 'APPROVED', 'ACC-10002',    'CURRENT', 'INR', 'IN', 'MEMBER', NOW(), NOW()),
  (UUID_TO_BIN('a1000003-3003-3003-3003-300000000003'), 'Neo Retail Pvt Ltd', 'neo.retail@rupeex.seed',      '+91-9100030003', NULL,         'APPROVED', 'ACC-10003',    'CURRENT', 'INR', 'IN', 'MEMBER', NOW(), NOW()),
  (UUID_TO_BIN('a1000004-4004-4004-4004-400000000004'), 'Zen Imports LLC',    'zen.imports@rupeex.seed',     '+91-9100040004', NULL,         'APPROVED', 'ACC-10004',    'CURRENT', 'USD', 'US', 'MEMBER', NOW(), NOW()),
  (UUID_TO_BIN('a1000005-5005-5005-5005-500000000005'), 'Lina Das',           'lina.das@rupeex.seed',        '+91-9100050005', '1995-11-30', 'APPROVED', 'ACC-10005',    'SAVINGS', 'INR', 'IN', 'MEMBER', NOW(), NOW()),
  (UUID_TO_BIN('a1000006-6006-6006-6006-600000000006'), 'Atlas Logistics',    'atlas.logistics@rupeex.seed', '+91-9100060006', NULL,         'APPROVED', 'ACC-10006',    'CURRENT', 'INR', 'IN', 'MEMBER', NOW(), NOW()),
  (UUID_TO_BIN('a0000000-0000-0000-0000-ad0000000001'), 'Platform Admin',     'admin@rupeex.seed',           '+91-9000000001', NULL,         'APPROVED', 'ACC-ADMIN-001','CURRENT', 'INR', 'IN', 'ADMIN',  NOW(), NOW());

-- Seed consents for all customers (TERMS_AND_PRIVACY accepted)
INSERT INTO consents (customer_id, consent_type, consent_version, accepted, accepted_at, created_at)
VALUES
  (UUID_TO_BIN('a1000001-1001-1001-1001-100000000001'), 'TERMS_AND_PRIVACY', 'v1.0', TRUE, NOW(), NOW()),
  (UUID_TO_BIN('a1000002-2002-2002-2002-200000000002'), 'TERMS_AND_PRIVACY', 'v1.0', TRUE, NOW(), NOW()),
  (UUID_TO_BIN('a1000003-3003-3003-3003-300000000003'), 'TERMS_AND_PRIVACY', 'v1.0', TRUE, NOW(), NOW()),
  (UUID_TO_BIN('a1000004-4004-4004-4004-400000000004'), 'TERMS_AND_PRIVACY', 'v1.0', TRUE, NOW(), NOW()),
  (UUID_TO_BIN('a1000005-5005-5005-5005-500000000005'), 'TERMS_AND_PRIVACY', 'v1.0', TRUE, NOW(), NOW()),
  (UUID_TO_BIN('a1000006-6006-6006-6006-600000000006'), 'TERMS_AND_PRIVACY', 'v1.0', TRUE, NOW(), NOW()),
  (UUID_TO_BIN('a0000000-0000-0000-0000-ad0000000001'), 'TERMS_AND_PRIVACY', 'v1.0', TRUE, NOW(), NOW());

-- ============================================================================
-- FRAUD RULES CONFIGURATION - All 7 Rule Types with Proper Settings
-- ============================================================================

-- Rule 1: LARGE_TRANSACTION
-- Threshold: Amount in currency (anything > 20,000 INR triggers rule)
-- When: Any transaction exceeding this amount
-- Score: 30 points (Medium-High risk)
-- Description: Captures high-value transactions for additional scrutiny
INSERT INTO fraud_rules (name, description, rule_type, threshold, score_contribution, enabled, created_at, updated_at)
VALUES
  ('RULE-001: Large Transaction', 'Detects transactions exceeding 20000 INR. High-value transfers warrant additional verification. Threshold: 20000. Score: 30.', 'LARGE_TRANSACTION', 20000.0, 30, TRUE, NOW(), NOW());

-- Rule 2: NIGHT_TRANSACTION
-- Threshold: 0 (N/A - time-based, always evaluates)
-- When: 22:00 to 06:00 local time
-- Score: 10 points (Low risk)
-- Description: Off-hours transactions may indicate compromised accounts
INSERT INTO fraud_rules (name, description, rule_type, threshold, score_contribution, enabled, created_at, updated_at)
VALUES
  ('RULE-002: Night Transaction', 'Detects transactions between 22:00 and 06:00. Off-hours activity may indicate account compromise. Threshold: N/A. Score: 10.', 'NIGHT_TRANSACTION', 0.0, 10, TRUE, NOW(), NOW());

-- Rule 3: VELOCITY_CHECK
-- Threshold: 5 (maximum 5 transactions allowed in 10-minute window)
-- When: Account attempts 5+ transactions in any 10-minute period
-- Score: 20 points (Medium risk)
-- Description: Detects burst/rapid activity patterns typical of account takeover
INSERT INTO fraud_rules (name, description, rule_type, threshold, score_contribution, enabled, created_at, updated_at)
VALUES
  ('RULE-003: Velocity Check', 'Detects burst transactions - more than 5 per 10 minutes from same account. Indicates rapid fraudulent activity. Threshold: 5. Score: 20.', 'VELOCITY_CHECK', 5.0, 20, TRUE, NOW(), NOW());

-- Rule 4: REPEATED_FAILED_ATTEMPTS
-- Threshold: 3 (flag when 3+ failures detected)
-- When: Account has 3 or more failed payment attempts
-- Score: 25 points (Medium-High risk)
-- Description: Multiple failures may indicate credential guessing or system abuse
INSERT INTO fraud_rules (name, description, rule_type, threshold, score_contribution, enabled, created_at, updated_at)
VALUES
  ('RULE-004: Repeated Failed Attempts', 'Detects accounts with 3+ consecutive failed payment attempts. May indicate credential compromise or system abuse. Threshold: 3. Score: 25.', 'REPEATED_FAILED_ATTEMPTS', 3.0, 25, TRUE, NOW(), NOW());

-- Rule 5: BLACKLISTED_ACCOUNT
-- Threshold: 0 (N/A - list-based check)
-- Blocked Accounts: BLK-FRAUD-001, BLK-FRAUD-002, BLK-SANCTION-101, BLK-MONEY-LAUND-001, BLK-COMPLIANCE-FAIL, BLK-SCAM-RING-05
-- When: Source OR destination account matches any blocked account
-- Score: 50 points (Critical/Automatic block)
-- Format: Comma-separated account numbers in description
INSERT INTO fraud_rules (name, description, rule_type, threshold, score_contribution, enabled, created_at, updated_at)
VALUES
  ('RULE-005: Blacklisted Account', 'BLK-FRAUD-001,BLK-FRAUD-002,BLK-SANCTION-101,BLK-MONEY-LAUND-001,BLK-COMPLIANCE-FAIL,BLK-SCAM-RING-05,BLK-TERROR-FINANCING,BLOCKED-ACCOUNT-999', 'BLACKLISTED_ACCOUNT', 0.0, 50, TRUE, NOW(), NOW());

-- Rule 6: HIGH_RISK_COUNTRY
-- Threshold: 0 (N/A - country-based check)
-- High-Risk Countries: KP (North Korea), IR (Iran), SY (Syria), CU (Cuba), ZW (Zimbabwe), MM (Myanmar), VE (Venezuela), BY (Belarus)
-- When: Source OR destination account's country code matches high-risk list
-- Score: 35 points (High risk)
-- Format: [Countries:CC1,CC2,CC3,...] in description (ISO 2-letter country codes)
INSERT INTO fraud_rules (name, description, rule_type, threshold, score_contribution, enabled, created_at, updated_at)
VALUES
  ('RULE-006: High Risk Country', '[Countries:KP,IR,SY,CU,ZW,MM,VE,BY]', 'HIGH_RISK_COUNTRY', 0.0, 35, TRUE, NOW(), NOW());

-- Rule 7: NEW_ACCOUNT
-- Threshold: 30 (account age limit in days - flag accounts created within 30 days)
-- When: Payment originates from account created within last 30 days
-- Score: 15 points (Low-Medium risk)
-- Description: New accounts are higher risk as they may be created for fraud
INSERT INTO fraud_rules (name, description, rule_type, threshold, score_contribution, enabled, created_at, updated_at)
VALUES
  ('RULE-007: New Account', 'Detects transactions from recently created accounts (within 30 days). New accounts may be fraudulent. Threshold: 30 days. Score: 15.', 'NEW_ACCOUNT', 30.0, 15, TRUE, NOW(), NOW());

-- Optional Rule 8: SUSPICIOUS_FREQUENCY (Disabled by default - complements VELOCITY_CHECK)
-- Threshold: 3 (flag if 3+ transactions per hour)
-- When: Account exceeds 3 transactions in any 1-hour window
-- Score: 15 points (Low-Medium risk)
INSERT INTO fraud_rules (name, description, rule_type, threshold, score_contribution, enabled, created_at, updated_at)
VALUES
  ('RULE-008: Suspicious Frequency', 'Detects high frequency transactions - more than 3 per hour. Complements velocity check. Threshold: 3/hour. Score: 15. (Optional)', 'SUSPICIOUS_FREQUENCY', 3.0, 15, FALSE, NOW(), NOW());

-- Seed payments (QUEUED and PROCESSING for testing, SENT/SETTLED for completed flows)
-- Note: SETTLED cannot be CANCELLED per PaymentStateMachine - only CREATED/QUEUED/PROCESSING can be
-- payer_email field captures the payer's email for notifications (resolution fallback to account.email)
INSERT INTO payments
  (payment_reference, amount, currency, source_account, destination_account, status, payer_email, error_code, error_message, idempotency_key, created_at, updated_at)
VALUES
  ('SEED-PAY-1001', 1500.00, 'INR', 'ACC-10001', 'ACC-10003', 'QUEUED', 'aarav.mehta@rupeex.seedaccount', NULL, NULL, 'seed-idem-1001', NOW(), NOW()),
  ('SEED-PAY-1002', 42000.00, 'INR', 'ACC-10002', 'ACC-10006', 'QUEUED', 'priya.sharma@rupeex.seedaccount', NULL, NULL, 'seed-idem-1002', NOW(), NOW()),
  ('SEED-PAY-1003', 980000.00, 'INR', 'ACC-10003', 'ACC-10005', 'PROCESSING', 'neo.retail@rupeex.seedaccount', NULL, NULL, 'seed-idem-1003', NOW(), NOW()),
  ('SEED-PAY-1004', 15500.75, 'USD', 'ACC-10004', 'ACC-10002', 'PROCESSING', 'zen.imports@rupeex.seedaccount', NULL, NULL, 'seed-idem-1004', NOW(), NOW()),
  ('SEED-PAY-1005', 8750.00, 'INR', 'ACC-10005', 'ACC-10001', 'SENT', 'lina.das@rupeex.seedaccount', NULL, NULL, 'seed-idem-1005', NOW(), NOW()),
  ('SEED-PAY-1006', 2300.00, 'INR', 'ACC-10006', 'ACC-10002', 'FAILED', 'atlas.logistics@rupeex.seedaccount', 'RISK_BLOCKED', 'Blocked by risk policy', 'seed-idem-1006', NOW(), NOW());

-- Seed payment history for traceability
INSERT INTO payment_history (payment_id, old_status, new_status, reason, changed_at)
SELECT p.id, 'CREATED', 'QUEUED', 'Payment queued for processing', NOW()
FROM payments p WHERE p.payment_reference = 'SEED-PAY-1001';

INSERT INTO payment_history (payment_id, old_status, new_status, reason, changed_at)
SELECT p.id, 'CREATED', 'QUEUED', 'Payment queued for processing', NOW()
FROM payments p WHERE p.payment_reference = 'SEED-PAY-1002';

INSERT INTO payment_history (payment_id, old_status, new_status, reason, changed_at)
SELECT p.id, 'CREATED', 'PROCESSING', 'Payment processing', NOW()
FROM payments p WHERE p.payment_reference = 'SEED-PAY-1003';

INSERT INTO payment_history (payment_id, old_status, new_status, reason, changed_at)
SELECT p.id, 'CREATED', 'PROCESSING', 'Payment processing', NOW()
FROM payments p WHERE p.payment_reference = 'SEED-PAY-1004';

INSERT INTO payment_history (payment_id, old_status, new_status, reason, changed_at)
SELECT p.id, 'CREATED', 'SENT', 'Payment sent to beneficiary', NOW()
FROM payments p WHERE p.payment_reference = 'SEED-PAY-1005';

INSERT INTO payment_history (payment_id, old_status, new_status, reason, changed_at)
SELECT p.id, 'CREATED', 'FAILED', 'Payment failed at validation', NOW()
FROM payments p WHERE p.payment_reference = 'SEED-PAY-1006';

-- Seed processing queue for QUEUED payments
INSERT INTO processing_queue (payment_id, status, retry_count, next_attempt_at, created_at, updated_at)
SELECT p.id, 'READY', 0, DATE_ADD(NOW(), INTERVAL 5 SECOND), NOW(), NOW()
FROM payments p WHERE p.payment_reference IN ('SEED-PAY-1001', 'SEED-PAY-1002');

-- Seed dead-letter queue for failed payment
INSERT INTO dead_letter_queue (payment_id, reason, last_retry_count, created_at)
SELECT p.id, 'Failed at validation stage', 0, NOW()
FROM payments p WHERE p.payment_reference = 'SEED-PAY-1006';

-- Seed risk score only for high-risk payment
INSERT INTO risk_scores (payment_id, score, category, explanation, decision, created_at)
SELECT p.id, 45, 'MEDIUM', 'Large amount flagged by fraud rules', 'REVIEW_PENDING', NOW()
FROM payments p WHERE p.payment_reference = 'SEED-PAY-1003';

COMMIT;
