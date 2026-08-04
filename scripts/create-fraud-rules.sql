-- Comprehensive Fraud Rules Configuration
-- This script creates all 7 fraud rules with proper thresholds and settings

DELETE FROM fraud_rules;

-- Rule 1: LARGE_TRANSACTION
-- Threshold = Amount in currency (20,000 INR = large transaction)
-- Triggers when payment amount exceeds threshold
INSERT INTO fraud_rules (name, description, rule_type, threshold, score_contribution, enabled, created_at, updated_at)
VALUES (
  'Large Transaction Rule',
  'Triggers for transactions exceeding 20,000 INR. High-value transfers require additional scrutiny.',
  'LARGE_TRANSACTION',
  20000.00,
  30,
  TRUE,
  NOW(),
  NOW()
);

-- Rule 2: NIGHT_TRANSACTION
-- Threshold = 0 (N/A - always evaluate time)
-- Triggers when transaction occurs between 22:00 and 06:00
INSERT INTO fraud_rules (name, description, rule_type, threshold, score_contribution, enabled, created_at, updated_at)
VALUES (
  'Night Transaction Rule',
  'Triggers for transactions between 22:00 and 06:00. Unusual timing may indicate fraud.',
  'NIGHT_TRANSACTION',
  0.00,
  10,
  TRUE,
  NOW(),
  NOW()
);

-- Rule 3: VELOCITY_CHECK
-- Threshold = Max transactions allowed in 10-minute window (5 transactions)
-- Triggers when account exceeds transaction velocity (burst activity)
INSERT INTO fraud_rules (name, description, rule_type, threshold, score_contribution, enabled, created_at, updated_at)
VALUES (
  'Velocity Check Rule',
  'Triggers when account attempts more than 5 transactions in a 10-minute window. Indicates rapid/burst activity.',
  'VELOCITY_CHECK',
  5.00,
  20,
  TRUE,
  NOW(),
  NOW()
);

-- Rule 4: REPEATED_FAILED_ATTEMPTS
-- Threshold = Max failed attempts before flagging (3 consecutive failures)
-- Triggers when account has 3+ failed payment attempts
INSERT INTO fraud_rules (name, description, rule_type, threshold, score_contribution, enabled, created_at, updated_at)
VALUES (
  'Repeated Failed Attempts Rule',
  'Triggers when account has 3 or more failed payment attempts. May indicate credential compromise.',
  'REPEATED_FAILED_ATTEMPTS',
  3.00,
  25,
  TRUE,
  NOW(),
  NOW()
);

-- Rule 5: BLACKLISTED_ACCOUNT
-- Description format: Comma-separated list of blacklisted account numbers
-- Triggers when source OR destination account is on the blocklist
INSERT INTO fraud_rules (name, description, rule_type, threshold, score_contribution, enabled, created_at, updated_at)
VALUES (
  'Blacklisted Account Rule',
  'BLK-FRAUD-001,BLK-FRAUD-002,BLK-SANCTION-101,BLK-MONEY-LAUND-05,BLOCKED-ACCOUNT-1,BLOCKED-ACCOUNT-2',
  'BLACKLISTED_ACCOUNT',
  0.00,
  50,
  TRUE,
  NOW(),
  NOW()
);

-- Rule 6: HIGH_RISK_COUNTRY
-- Description format: [Countries:CC1,CC2,CC3,...] with ISO country codes
-- Triggers when source OR destination account is from high-risk country
INSERT INTO fraud_rules (name, description, rule_type, threshold, score_contribution, enabled, created_at, updated_at)
VALUES (
  'High Risk Country Rule',
  '[Countries:KP,IR,SY,CU,ZW,MM,VE,BY]',
  'HIGH_RISK_COUNTRY',
  0.00,
  35,
  TRUE,
  NOW(),
  NOW()
);

-- Rule 7: NEW_ACCOUNT
-- Threshold = Account age limit in days (30 days = accounts created within last month)
-- Triggers when transaction originates from newly created account
INSERT INTO fraud_rules (name, description, rule_type, threshold, score_contribution, enabled, created_at, updated_at)
VALUES (
  'New Account Rule',
  'Triggers for transactions from accounts created within the last 30 days. New accounts are higher risk.',
  'NEW_ACCOUNT',
  30.00,
  15,
  TRUE,
  NOW(),
  NOW()
);

-- Rule 8: SUSPICIOUS_FREQUENCY (Optional)
-- Similar to VELOCITY_CHECK but with different thresholds
INSERT INTO fraud_rules (name, description, rule_type, threshold, score_contribution, enabled, created_at, updated_at)
VALUES (
  'Suspicious Frequency Rule',
  'Triggers for accounts with more than 3 transactions in 1 hour. Complements velocity check.',
  'SUSPICIOUS_FREQUENCY',
  3.00,
  15,
  FALSE,
  NOW(),
  NOW()
);

-- Display all configured rules
SELECT * FROM fraud_rules ORDER BY score_contribution DESC;
