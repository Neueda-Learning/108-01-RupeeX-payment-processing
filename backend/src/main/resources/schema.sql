-- Payment Processing & Risk Intelligence Platform
-- MySQL normalized reference schema

CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(100) NOT NULL UNIQUE,
    account_holder VARCHAR(255) NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    country_code VARCHAR(2),
    balance DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    email VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_reference VARCHAR(128) NOT NULL UNIQUE,
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    source_account VARCHAR(100) NOT NULL,
    destination_account VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    error_code VARCHAR(100),
    error_message VARCHAR(1000),
    payer_email VARCHAR(255),
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    source_currency VARCHAR(3),
    destination_currency VARCHAR(3),
    converted_amount DECIMAL(19,2),
    exchange_rate DECIMAL(19,6),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_payments_status (status),
    INDEX idx_payments_created_at (created_at),
    INDEX idx_payer_email (payer_email)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS payment_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    old_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    reason VARCHAR(1000),
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_payment_history_payment_id (payment_id),
    CONSTRAINT fk_payment_history_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT,
    service VARCHAR(120) NOT NULL,
    action VARCHAR(255) NOT NULL,
    before_state VARCHAR(50),
    after_state VARCHAR(50),
    processing_time_ms BIGINT,
    reason VARCHAR(1000),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_payment_id (payment_id),
    CONSTRAINT fk_audit_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS fraud_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(1000) NOT NULL,
    rule_type VARCHAR(80) NOT NULL,
    threshold DOUBLE NOT NULL DEFAULT 0,
    score_contribution INT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS fraud_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    rule_id BIGINT NOT NULL,
    rule_name VARCHAR(120) NOT NULL,
    triggered BOOLEAN NOT NULL,
    score_contribution INT NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_fraud_payment_id (payment_id),
    CONSTRAINT fk_fraud_result_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE,
    CONSTRAINT fk_fraud_result_rule FOREIGN KEY (rule_id) REFERENCES fraud_rules(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS risk_scores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL UNIQUE,
    score INT NOT NULL,
    category VARCHAR(50) NOT NULL,
    explanation VARCHAR(2000) NOT NULL,
    decision VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_risk_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS processing_queue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL UNIQUE,
    status VARCHAR(40) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_queue_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS dead_letter_queue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL UNIQUE,
    reason VARCHAR(1000) NOT NULL,
    last_retry_count INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dlq_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT,
    type VARCHAR(80) NOT NULL,
    payload VARCHAR(2000) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS payment_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DOUBLE NOT NULL,
    recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS system_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    entity_id BIGINT,
    payload VARCHAR(2000) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS payment_verifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL UNIQUE,
    customer_id VARCHAR(100) NOT NULL,
    verification_token VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    customer_email VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_verification_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Safety net: Hibernate 6 + MySQLDialect maps @Enumerated(EnumType.STRING)
-- fields to a native MySQL ENUM(...) column by default (fixed value list
-- baked in at table-creation time) unless
-- hibernate.type.preferred_enum_jdbc_type=VARCHAR is set. Any table created
-- before that property existed may still have a native ENUM column, which
-- rejects newly added enum constants (e.g. a new PaymentStatus value) with
-- "Data truncated for column ...". These MODIFY statements are idempotent and
-- safe to re-run on every startup; they force the columns back to plain
-- VARCHAR so new enum values are always accepted.
ALTER TABLE payments MODIFY COLUMN status VARCHAR(50) NOT NULL;
ALTER TABLE payment_history MODIFY COLUMN old_status VARCHAR(50) NULL;
ALTER TABLE payment_history MODIFY COLUMN new_status VARCHAR(50) NOT NULL;
ALTER TABLE audit_logs MODIFY COLUMN before_state VARCHAR(50) NULL;
ALTER TABLE audit_logs MODIFY COLUMN after_state VARCHAR(50) NULL;
ALTER TABLE risk_scores MODIFY COLUMN category VARCHAR(50) NOT NULL;
ALTER TABLE fraud_rules MODIFY COLUMN rule_type VARCHAR(80) NOT NULL;
ALTER TABLE payment_verifications MODIFY COLUMN status VARCHAR(50) NOT NULL;

-- Data repair: MySQL's legacy zero-date sentinel ('0000-00-00 00:00:00') can
-- end up in DATETIME/TIMESTAMP columns (e.g. rows inserted without an
-- explicit value on a column that predates its DEFAULT CURRENT_TIMESTAMP, or
-- data imported/restored from a dump with strict mode disabled). Reading such
-- a value back throws "Zero date value prohibited" in MySQL Connector/J,
-- failing the entire query. These UPDATEs are idempotent (only rows currently
-- holding the sentinel are touched) and repair the data at the source, in
-- addition to the zeroDateTimeBehavior=CONVERT_TO_NULL JDBC URL option which
-- only masks the symptom on read.
--
-- MySQL 8's default sql_mode includes STRICT_TRANS_TABLES, which folds in the
-- legacy NO_ZERO_DATE behavior: even using '0000-00-00 00:00:00' as a literal
-- for comparison (not just for insert) is rejected with "Incorrect datetime
-- value". Temporarily relax sql_mode for the remainder of this session so the
-- comparisons below are allowed, then restore the original mode.
SET @rupeex_original_sql_mode = @@SESSION.sql_mode;
SET SESSION sql_mode = '';

UPDATE accounts SET created_at = NOW() WHERE created_at = '0000-00-00 00:00:00';
UPDATE accounts SET updated_at = NOW() WHERE updated_at = '0000-00-00 00:00:00';
UPDATE payments SET created_at = NOW() WHERE created_at = '0000-00-00 00:00:00';
UPDATE payments SET updated_at = NOW() WHERE updated_at = '0000-00-00 00:00:00';
UPDATE payment_history SET changed_at = NOW() WHERE changed_at = '0000-00-00 00:00:00';
UPDATE audit_logs SET created_at = NOW() WHERE created_at = '0000-00-00 00:00:00';
UPDATE fraud_rules SET created_at = NOW() WHERE created_at = '0000-00-00 00:00:00';
UPDATE fraud_rules SET updated_at = NOW() WHERE updated_at = '0000-00-00 00:00:00';
UPDATE fraud_results SET created_at = NOW() WHERE created_at = '0000-00-00 00:00:00';
UPDATE risk_scores SET created_at = NOW() WHERE created_at = '0000-00-00 00:00:00';
UPDATE processing_queue SET next_attempt_at = NOW() WHERE next_attempt_at = '0000-00-00 00:00:00';
UPDATE processing_queue SET created_at = NOW() WHERE created_at = '0000-00-00 00:00:00';
UPDATE processing_queue SET updated_at = NOW() WHERE updated_at = '0000-00-00 00:00:00';
UPDATE dead_letter_queue SET created_at = NOW() WHERE created_at = '0000-00-00 00:00:00';
UPDATE notifications SET created_at = NOW() WHERE created_at = '0000-00-00 00:00:00';
UPDATE payment_metrics SET recorded_at = NOW() WHERE recorded_at = '0000-00-00 00:00:00';
UPDATE system_events SET created_at = NOW() WHERE created_at = '0000-00-00 00:00:00';
UPDATE payment_verifications SET created_at = NOW() WHERE created_at = '0000-00-00 00:00:00';
UPDATE payment_verifications SET updated_at = NOW() WHERE updated_at = '0000-00-00 00:00:00';

SET SESSION sql_mode = @rupeex_original_sql_mode;

INSERT IGNORE INTO fraud_rules (name, description, rule_type, threshold, score_contribution, enabled)
VALUES
('Large Transaction', 'Flags transactions above threshold', 'LARGE_TRANSACTION', 20000, 30, TRUE),
('Night Transaction', 'Flags late-night transactions', 'NIGHT_TRANSACTION', 0, 10, TRUE),
('Velocity Check', 'Flags burst transactions in short window', 'VELOCITY_CHECK', 10, 20, TRUE),
('High Risk Country', 'Flags origin from sanctioned/high-risk countries', 'HIGH_RISK_COUNTRY', 0, 15, TRUE);
