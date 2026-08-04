CREATE TABLE IF NOT EXISTS customers (
    id BINARY(16) PRIMARY KEY,
    external_ref VARCHAR(100),
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL UNIQUE,
    dob DATE,
    status VARCHAR(50) NOT NULL,
    account_number VARCHAR(100) UNIQUE,
    account_type VARCHAR(50),
    currency VARCHAR(3),
    country_code VARCHAR(2),
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_customer_status (status),
    INDEX idx_customer_email (email),
    INDEX idx_customer_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS consents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BINARY(16) NOT NULL,
    consent_type VARCHAR(100) NOT NULL,
    consent_version VARCHAR(30) NOT NULL,
    accepted BOOLEAN NOT NULL,
    accepted_at DATETIME,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    INDEX idx_consent_customer_id (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

