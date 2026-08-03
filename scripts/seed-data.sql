-- RupeeX seed data (clean & idempotent)
-- Deletes previous seed data and rebuilds from scratch to avoid conflicts

START TRANSACTION;

-- Clean up previous seed data (identify by payment_reference prefix)
DELETE FROM risk_scores WHERE payment_id IN (SELECT id FROM payments WHERE payment_reference LIKE 'SEED-PAY-%');
DELETE FROM dead_letter_queue WHERE payment_id IN (SELECT id FROM payments WHERE payment_reference LIKE 'SEED-PAY-%');
DELETE FROM processing_queue WHERE payment_id IN (SELECT id FROM payments WHERE payment_reference LIKE 'SEED-PAY-%');
DELETE FROM payment_history WHERE payment_id IN (SELECT id FROM payments WHERE payment_reference LIKE 'SEED-PAY-%');
DELETE FROM notifications WHERE payment_id IN (SELECT id FROM payments WHERE payment_reference LIKE 'SEED-PAY-%');
DELETE FROM system_events WHERE entity_id IN (SELECT id FROM payments WHERE payment_reference LIKE 'SEED-PAY-%');
DELETE FROM payments WHERE payment_reference LIKE 'SEED-PAY-%';
DELETE FROM fraud_rules WHERE name LIKE 'Large Transaction%' OR name LIKE 'Night Transaction%' OR name LIKE 'Velocity Check%' OR name LIKE 'High Risk Country%' OR name LIKE 'Repeated Failed%';
DELETE FROM accounts WHERE account_number LIKE 'ACC-1000%';

-- Seed accounts
INSERT INTO accounts (account_number, account_holder, account_type, currency, country_code, balance, status, created_at, updated_at)
VALUES
  ('ACC-10001', 'Aarav Mehta',        'SAVINGS',  'INR', 'IN', 125000.00, 'ACTIVE', NOW(), NOW()),
  ('ACC-10002', 'Priya Sharma',       'CURRENT',  'INR', 'IN', 340000.00, 'ACTIVE', NOW(), NOW()),
  ('ACC-10003', 'Neo Retail Pvt Ltd', 'CURRENT',  'INR', 'IN', 870000.00, 'ACTIVE', NOW(), NOW()),
  ('ACC-10004', 'Zen Imports LLC',    'CURRENT',  'USD', 'US',  48000.00, 'ACTIVE', NOW(), NOW()),
  ('ACC-10005', 'Lina Das',           'SAVINGS',  'INR', 'IN',  62500.00, 'ACTIVE', NOW(), NOW()),
  ('ACC-10006', 'Atlas Logistics',    'CURRENT',  'INR', 'IN', 215000.00, 'ACTIVE', NOW(), NOW());

-- Seed fraud rules
INSERT INTO fraud_rules (name, description, rule_type, threshold, score_contribution, enabled, created_at, updated_at)
VALUES
  ('Large Transaction', 'Flags transactions above threshold', 'LARGE_TRANSACTION', 20000, 30, TRUE, NOW(), NOW()),
  ('Night Transaction', 'Flags late-night transactions', 'NIGHT_TRANSACTION', 0, 10, TRUE, NOW(), NOW()),
  ('Velocity Check', 'Flags burst transactions in short window', 'VELOCITY_CHECK', 10, 20, TRUE, NOW(), NOW()),
  ('High Risk Country', 'Flags origin from sanctioned/high-risk countries', 'HIGH_RISK_COUNTRY', 0, 15, TRUE, NOW(), NOW()),
  ('Repeated Failed Attempts', 'Flags repeated failed attempts', 'REPEATED_FAILED_ATTEMPTS', 3, 25, TRUE, NOW(), NOW());

-- Seed payments (QUEUED and PROCESSING for testing, SENT/SETTLED for completed flows)
-- Note: SETTLED cannot be CANCELLED per PaymentStateMachine - only CREATED/QUEUED/PROCESSING can be
INSERT INTO payments
  (payment_reference, amount, currency, source_account, destination_account, status, error_code, error_message, idempotency_key, created_at, updated_at)
VALUES
  ('SEED-PAY-1001', 1500.00, 'INR', 'ACC-10001', 'ACC-10003', 'QUEUED', NULL, NULL, 'seed-idem-1001', NOW(), NOW()),
  ('SEED-PAY-1002', 42000.00, 'INR', 'ACC-10002', 'ACC-10006', 'QUEUED', NULL, NULL, 'seed-idem-1002', NOW(), NOW()),
  ('SEED-PAY-1003', 980000.00, 'INR', 'ACC-10003', 'ACC-10005', 'PROCESSING', NULL, NULL, 'seed-idem-1003', NOW(), NOW()),
  ('SEED-PAY-1004', 15500.75, 'USD', 'ACC-10004', 'ACC-10002', 'PROCESSING', NULL, NULL, 'seed-idem-1004', NOW(), NOW()),
  ('SEED-PAY-1005', 8750.00, 'INR', 'ACC-10005', 'ACC-10001', 'SENT', NULL, NULL, 'seed-idem-1005', NOW(), NOW()),
  ('SEED-PAY-1006', 2300.00, 'INR', 'ACC-10006', 'ACC-10002', 'FAILED', 'RISK_BLOCKED', 'Blocked by risk policy', 'seed-idem-1006', NOW(), NOW());

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
