-- RupeeX seed data (idempotent)
-- Safe to run multiple times.

START TRANSACTION;

INSERT INTO accounts (account_number, account_holder, account_type, currency, country_code, status)
VALUES
  ('ACC-10001', 'Aarav Mehta', 'SAVINGS', 'INR', 'IN', 'ACTIVE'),
  ('ACC-10002', 'Priya Sharma', 'CURRENT', 'INR', 'IN', 'ACTIVE'),
  ('ACC-10003', 'Neo Retail Pvt Ltd', 'CURRENT', 'INR', 'IN', 'ACTIVE'),
  ('ACC-10004', 'Zen Imports LLC', 'CURRENT', 'USD', 'US', 'ACTIVE'),
  ('ACC-10005', 'Lina Das', 'SAVINGS', 'INR', 'IN', 'ACTIVE'),
  ('ACC-10006', 'Atlas Logistics', 'CURRENT', 'INR', 'IN', 'ACTIVE')
ON DUPLICATE KEY UPDATE
  account_holder = VALUES(account_holder),
  account_type = VALUES(account_type),
  currency = VALUES(currency),
  country_code = VALUES(country_code),
  status = VALUES(status),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO fraud_rules (name, description, rule_type, threshold, score_contribution, enabled)
VALUES
  ('Large Transaction', 'Flags transactions above threshold', 'LARGE_TRANSACTION', 20000, 30, TRUE),
  ('Night Transaction', 'Flags late-night transactions', 'NIGHT_TRANSACTION', 0, 10, TRUE),
  ('Velocity Check', 'Flags burst transactions in short window', 'VELOCITY_CHECK', 10, 20, TRUE),
  ('High Risk Country', 'Flags origin from sanctioned/high-risk countries', 'HIGH_RISK_COUNTRY', 0, 15, TRUE),
  ('Repeated Failed Attempts', 'Flags repeated failed attempts', 'REPEATED_FAILED_ATTEMPTS', 3, 25, TRUE)
ON DUPLICATE KEY UPDATE
  description = VALUES(description),
  rule_type = VALUES(rule_type),
  threshold = VALUES(threshold),
  score_contribution = VALUES(score_contribution),
  enabled = VALUES(enabled),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO payments
  (payment_reference, amount, currency, source_account, destination_account, status, error_code, error_message, idempotency_key)
VALUES
  ('SEED-PAY-1001', 1500.00, 'INR', 'ACC-10001', 'ACC-10003', 'SETTLED', NULL, NULL, 'seed-idem-1001'),
  ('SEED-PAY-1002', 42000.00, 'INR', 'ACC-10002', 'ACC-10006', 'QUEUED', NULL, NULL, 'seed-idem-1002'),
  ('SEED-PAY-1003', 980000.00, 'INR', 'ACC-10003', 'ACC-10005', 'FAILED', 'RISK_BLOCKED', 'Blocked by risk policy', 'seed-idem-1003'),
  ('SEED-PAY-1004', 15500.75, 'USD', 'ACC-10004', 'ACC-10002', 'PROCESSING', NULL, NULL, 'seed-idem-1004'),
  ('SEED-PAY-1005', 8750.00, 'INR', 'ACC-10005', 'ACC-10001', 'SETTLED', NULL, NULL, 'seed-idem-1005'),
  ('SEED-PAY-1006', 2300.00, 'INR', 'ACC-10006', 'ACC-10002', 'SENT', NULL, NULL, 'seed-idem-1006')
ON DUPLICATE KEY UPDATE
  amount = VALUES(amount),
  currency = VALUES(currency),
  source_account = VALUES(source_account),
  destination_account = VALUES(destination_account),
  status = VALUES(status),
  error_code = VALUES(error_code),
  error_message = VALUES(error_message),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO payment_history (payment_id, old_status, new_status, reason)
SELECT p.id, NULL, 'CREATED', 'Seeded payment'
FROM payments p
WHERE p.payment_reference = 'SEED-PAY-1001'
  AND NOT EXISTS (
    SELECT 1 FROM payment_history h
    WHERE h.payment_id = p.id AND h.new_status = 'CREATED'
  );

INSERT INTO payment_history (payment_id, old_status, new_status, reason)
SELECT p.id, 'CREATED', 'SETTLED', 'Seeded as completed payment'
FROM payments p
WHERE p.payment_reference = 'SEED-PAY-1001'
  AND NOT EXISTS (
    SELECT 1 FROM payment_history h
    WHERE h.payment_id = p.id AND h.new_status = 'SETTLED'
  );

INSERT INTO payment_history (payment_id, old_status, new_status, reason)
SELECT p.id, NULL, 'FAILED', 'Seeded as failed payment for DLQ/testing'
FROM payments p
WHERE p.payment_reference = 'SEED-PAY-1003'
  AND NOT EXISTS (
    SELECT 1 FROM payment_history h
    WHERE h.payment_id = p.id AND h.new_status = 'FAILED'
  );

INSERT INTO processing_queue (payment_id, status, retry_count, next_attempt_at)
SELECT p.id, 'READY', 1, DATE_ADD(NOW(), INTERVAL 15 SECOND)
FROM payments p
WHERE p.payment_reference = 'SEED-PAY-1002'
ON DUPLICATE KEY UPDATE
  status = VALUES(status),
  retry_count = VALUES(retry_count),
  next_attempt_at = VALUES(next_attempt_at),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO dead_letter_queue (payment_id, reason, last_retry_count)
SELECT p.id, 'Exceeded retry limit (seed sample)', 3
FROM payments p
WHERE p.payment_reference = 'SEED-PAY-1003'
ON DUPLICATE KEY UPDATE
  reason = VALUES(reason),
  last_retry_count = VALUES(last_retry_count);

INSERT INTO risk_scores (payment_id, score, category, explanation, decision)
SELECT p.id, 78, 'HIGH', 'Large amount and suspicious pattern (seed sample)', 'MANUAL_REVIEW'
FROM payments p
WHERE p.payment_reference = 'SEED-PAY-1003'
ON DUPLICATE KEY UPDATE
  score = VALUES(score),
  category = VALUES(category),
  explanation = VALUES(explanation),
  decision = VALUES(decision);

INSERT INTO notifications (payment_id, type, payload)
SELECT p.id, 'PAYMENT_CREATED', '{"paymentReference":"SEED-PAY-1002","status":"QUEUED"}'
FROM payments p
WHERE p.payment_reference = 'SEED-PAY-1002'
  AND NOT EXISTS (
    SELECT 1 FROM notifications n
    WHERE n.payment_id = p.id AND n.type = 'PAYMENT_CREATED'
  );

INSERT INTO system_events (event_type, entity_id, payload)
SELECT 'PAYMENT_SEEDED', p.id, CONCAT('{"paymentReference":"', p.payment_reference, '","status":"', p.status, '"}')
FROM payments p
WHERE p.payment_reference IN ('SEED-PAY-1001', 'SEED-PAY-1002', 'SEED-PAY-1003')
  AND NOT EXISTS (
    SELECT 1 FROM system_events e
    WHERE e.entity_id = p.id AND e.event_type = 'PAYMENT_SEEDED'
  );

INSERT INTO payment_metrics (metric_name, metric_value)
VALUES
  ('seed.total_payments', 6),
  ('seed.failed_payments', 1),
  ('seed.queued_payments', 1);

COMMIT;
