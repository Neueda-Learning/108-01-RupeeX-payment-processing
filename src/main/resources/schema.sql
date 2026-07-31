-- RupeeX Payment Processing Database Schema
-- This file documents the database structure
-- Note: Tables will be auto-created by Hibernate on application startup
-- (see spring.jpa.hibernate.ddl-auto=update in application.properties)

-- ============================================================================
-- 1. PAYMENTS TABLE
-- ============================================================================
-- Stores all payment transactions
--
-- Field              Purpose                          Type
-- id                 Unique payment identifier        BIGINT (Auto-increment)
-- amount             Payment amount                   DECIMAL(19,4)
-- currency           ISO 4217 Currency Code           VARCHAR(3)
-- source_account     Sender account identifier        VARCHAR(255)
-- destination_account Receiver account identifier     VARCHAR(255)
-- reference          Payment description/reference    VARCHAR(500)
-- status             Current payment state            VARCHAR(50)
-- error_code         Failure reason (if failed)       VARCHAR(100)
-- idempotency_key    Unique key for duplicate prevention VARCHAR(100)
-- created_at         Transaction creation timestamp   DATETIME
-- updated_at         Transaction last updated         DATETIME
--
-- Possible Status Values: PENDING, INITIATED, PROCESSING, COMPLETED, FAILED, CANCELLED

CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    source_account VARCHAR(255) NOT NULL,
    destination_account VARCHAR(255) NOT NULL,
    reference VARCHAR(500),
    status VARCHAR(50) NOT NULL,
    error_code VARCHAR(100),
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_idempotency_key (idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 2. PAYMENT_STATUS_HISTORY TABLE
-- ============================================================================
-- Tracks all status changes for each payment (audit trail)
--
-- Field              Purpose                          Type
-- id                 Unique history record ID         BIGINT (Auto-increment)
-- payment_id         Reference to payments.id         BIGINT
-- status             The status at this moment        VARCHAR(50)
-- changed_at         When the status changed          DATETIME
-- remarks            Additional notes/comments        VARCHAR(500)
-- changed_by         Who/what system changed it       VARCHAR(100)

CREATE TABLE IF NOT EXISTS payment_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remarks VARCHAR(500),
    changed_by VARCHAR(100),
    INDEX idx_payment_id (payment_id),
    INDEX idx_changed_at (changed_at),
    FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 3. ACCOUNTS TABLE
-- ============================================================================
-- Stores all bank accounts (both source and destination)
-- Can represent sender/receiver accounts in the system
--
-- Field              Purpose                          Type
-- id                 Unique account identifier        BIGINT (Auto-increment)
-- account_number     Account number                   VARCHAR(100) UNIQUE
-- account_holder     Account holder name              VARCHAR(255)
-- account_type       Account type (SAVINGS/CHECKING)  VARCHAR(50)
-- currency           Account currency (ISO 4217)      VARCHAR(3)
-- bank_name          Bank name                        VARCHAR(100)
-- bank_code          Bank code/routing number         VARCHAR(20)
-- ifsc_code          IFSC code (India Standard)       VARCHAR(50)
-- swift_code         SWIFT code (International)       VARCHAR(50)
-- status             Account status (ACTIVE/INACTIVE) VARCHAR(50)
-- metadata           Additional JSON metadata         VARCHAR(500)
-- created_at         Account creation timestamp       DATETIME
-- updated_at         Account last updated             DATETIME

CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(100) NOT NULL UNIQUE,
    account_holder VARCHAR(255) NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    bank_name VARCHAR(100),
    bank_code VARCHAR(20),
    ifsc_code VARCHAR(50),
    swift_code VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    metadata VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_account_number (account_number),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- REFERENCE DATA & EXAMPLES
-- ============================================================================

-- Example: Insert sample accounts
-- INSERT INTO accounts (account_number, account_holder, account_type, currency, bank_name, ifsc_code, status)
-- VALUES
--   ('ACC001001', 'John Doe', 'SAVINGS', 'INR', 'HDFC Bank', 'HDFC0001234', 'ACTIVE'),
--   ('ACC002001', 'Jane Smith', 'CHECKING', 'INR', 'ICICI Bank', 'ICIC0002345', 'ACTIVE');

-- Example: Insert sample payment
-- INSERT INTO payments (amount, currency, source_account, destination_account, reference, status, idempotency_key)
-- VALUES (5000.00, 'INR', 'ACC001001', 'ACC002001', 'Monthly salary transfer', 'PENDING', UUID());

-- Example: Insert payment status history
-- INSERT INTO payment_status_history (payment_id, status, remarks, changed_by)
-- VALUES (1, 'PENDING', 'Payment queued for processing', 'SYSTEM');

