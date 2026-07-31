# RupeeX Payment Processing - Database Schema Documentation

## Overview
This document describes the three tables created for the RupeeX payment processing system:
1. **Payments** - Main payment transactions
2. **PaymentStatusHistory** - Audit trail of payment status changes
3. **Accounts** - Account management (source and destination)

---

## 1. PAYMENTS Table

### Purpose
Stores all payment transactions in the system.

### Fields

| Field | Type | Constraints | Purpose |
|-------|------|-----------|---------|
| `id` | BIGINT | PK, Auto-increment | Unique payment identifier |
| `amount` | DECIMAL(19,4) | NOT NULL | Payment amount (supports up to 15 digits with 4 decimal places) |
| `currency` | VARCHAR(3) | NOT NULL | ISO 4217 Currency Code (e.g., INR, USD) |
| `source_account` | VARCHAR(255) | NOT NULL | Sender's account identifier |
| `destination_account` | VARCHAR(255) | NOT NULL | Receiver's account identifier |
| `reference` | VARCHAR(500) | NULLABLE | Payment description/reference |
| `status` | VARCHAR(50) | NOT NULL | Current payment state |
| `error_code` | VARCHAR(100) | NULLABLE | Failure reason (only populated on failure) |
| `idempotency_key` | VARCHAR(100) | UNIQUE, NOT NULL | Unique key to prevent duplicate payments |
| `created_at` | DATETIME | NOT NULL | Transaction creation timestamp (immutable) |
| `updated_at` | DATETIME | NOT NULL | Transaction last modified timestamp |

### Indexes
- `idx_status` - For querying by payment status
- `idx_created_at` - For sorting and date range queries
- `idx_idempotency_key` - For duplicate prevention lookups

### Status Values
- `PENDING` - Payment awaiting initiation
- `INITIATED` - Payment processing has started
- `PROCESSING` - Payment is being processed
- `COMPLETED` - Payment successfully completed
- `FAILED` - Payment failed (see error_code)
- `CANCELLED` - Payment was cancelled

### Example Java Code
```java
// Creating a payment
Payments payment = new Payments(
    new BigDecimal("5000.00"),  // amount
    "INR",                        // currency
    "ACC001001",                  // source_account
    "ACC002001",                  // destination_account
    "Monthly salary",             // reference
    "PENDING",                    // status
    UUID.randomUUID().toString()  // idempotency_key
);

// Save using repository
paymentsRepository.save(payment);

// Find by idempotency key
Optional<Payments> existingPayment = paymentsRepository.findByIdempotencyKey(idempotencyKey);
```

---

## 2. PAYMENT_STATUS_HISTORY Table

### Purpose
Maintains an audit trail of all status changes for each payment. This is critical for:
- Compliance and regulatory requirements
- Debugging payment issues
- Understanding payment lifecycle

### Fields

| Field | Type | Constraints | Purpose |
|-------|------|-----------|---------|
| `id` | BIGINT | PK, Auto-increment | Unique history record ID |
| `payment_id` | BIGINT | NOT NULL, FK | Reference to payments.id |
| `status` | VARCHAR(50) | NOT NULL | Payment status at this moment |
| `changed_at` | DATETIME | NOT NULL | Timestamp when status changed |
| `remarks` | VARCHAR(500) | NULLABLE | Additional notes/comments |
| `changed_by` | VARCHAR(100) | NULLABLE | System/user that triggered change |

### Indexes
- `idx_payment_id` - For finding all history records for a payment
- `idx_changed_at` - For time-based queries

### Foreign Keys
- `FK payment_id → payments.id` (ON DELETE CASCADE)

### Example Java Code
```java
// Recording a status change
PaymentStatusHistory history = new PaymentStatusHistory(
    payment.getId(),              // payment_id
    "PROCESSING",                 // status
    "Initiated payment processing", // remarks
    "PAYMENT_ENGINE"              // changed_by
);

// Save using repository
historyRepository.save(history);

// Get all history for a payment (latest first)
List<PaymentStatusHistory> history = historyRepository
    .findByPaymentIdOrderByChangedAtDesc(paymentId);
```

---

## 3. ACCOUNTS Table

### Purpose
Stores all bank accounts used in the platform. Can represent both sender accounts (source) and receiver accounts (destination).

### Fields

| Field | Type | Constraints | Purpose |
|-------|------|-----------|---------|
| `id` | BIGINT | PK, Auto-increment | Unique account identifier |
| `account_number` | VARCHAR(100) | UNIQUE, NOT NULL | Account number |
| `account_holder` | VARCHAR(255) | NOT NULL | Account holder name |
| `account_type` | VARCHAR(50) | NOT NULL | Type (SAVINGS, CHECKING, CURRENT) |
| `currency` | VARCHAR(3) | NOT NULL | Account currency (ISO 4217) |
| `bank_name` | VARCHAR(100) | NULLABLE | Bank name |
| `bank_code` | VARCHAR(20) | NULLABLE | Routing number/bank code |
| `ifsc_code` | VARCHAR(50) | NULLABLE | IFSC code (Indian Standard) |
| `swift_code` | VARCHAR(50) | NULLABLE | SWIFT code (International) |
| `status` | VARCHAR(50) | NOT NULL, DEFAULT='ACTIVE' | Account status (ACTIVE, INACTIVE, SUSPENDED) |
| `metadata` | VARCHAR(500) | NULLABLE | Additional JSON metadata |
| `created_at` | DATETIME | NOT NULL | Account creation timestamp |
| `updated_at` | DATETIME | NOT NULL | Account last modified timestamp |

### Indexes
- `idx_account_number` - For quick account lookups
- `idx_status` - For finding active accounts
- `idx_created_at` - For account registration date queries

### Example Java Code
```java
// Creating an account
Accounts account = new Accounts(
    "ACC001001",           // account_number
    "John Doe",            // account_holder
    "SAVINGS",             // account_type
    "INR",                 // currency
    "HDFC Bank"            // bank_name
);
account.setIfscCode("HDFC0001234");
account.setSwiftCode("HDFCINBB");

// Save using repository
accountsRepository.save(account);

// Find account by number
Optional<Accounts> account = accountsRepository.findByAccountNumber("ACC001001");

// Find active account
Optional<Accounts> activeAccount = accountsRepository
    .findByAccountNumberAndStatus("ACC001001", "ACTIVE");
```

---

## Database Auto-Creation

The application uses Hibernate with `spring.jpa.hibernate.ddl-auto=update`, which means:
1. Tables are automatically created on first run
2. Schema changes are applied automatically
3. No manual SQL execution is required

### Configuration (application.properties)
```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.jpa.properties.hibernate.format_sql=true
```

---

## Relationships

```
Payments (1) ----< (Many) PaymentStatusHistory
   |
   +-- Stores reference to Accounts (source_account)
   +-- Stores reference to Accounts (destination_account)

Accounts can be used as:
   - Source account in payments (sender)
   - Destination account in payments (receiver)
```

---

## Usage Patterns

### Pattern 1: Create a Payment
```java
// 1. Verify accounts exist
Accounts sourceAcc = accountsRepository.findByAccountNumber("ACC001001").orElseThrow();
Accounts destAcc = accountsRepository.findByAccountNumber("ACC002001").orElseThrow();

// 2. Check for duplicate using idempotency key
String idempotencyKey = generateIdempotencyKey(); // Should be unique per request
if (paymentsRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
    return "Payment already exists";
}

// 3. Create payment
Payments payment = new Payments(
    new BigDecimal("5000.00"),
    "INR",
    "ACC001001",
    "ACC002001",
    "Monthly salary",
    "PENDING",
    idempotencyKey
);

// 4. Save payment
paymentsRepository.save(payment);

// 5. Record initial status
PaymentStatusHistory history = new PaymentStatusHistory(
    payment.getId(),
    "PENDING",
    "Payment created",
    "USER"
);
historyRepository.save(history);
```

### Pattern 2: Update Payment Status
```java
Payments payment = paymentsRepository.findById(paymentId).orElseThrow();
payment.setStatus("PROCESSING");
paymentsRepository.save(payment);

PaymentStatusHistory history = new PaymentStatusHistory(
    payment.getId(),
    "PROCESSING",
    "Started processing at payment gateway",
    "PAYMENT_ENGINE"
);
historyRepository.save(history);
```

### Pattern 3: Handle Payment Failure
```java
Payments payment = paymentsRepository.findById(paymentId).orElseThrow();
payment.setStatus("FAILED");
payment.setErrorCode("INSUFFICIENT_FUNDS");
paymentsRepository.save(payment);

PaymentStatusHistory history = new PaymentStatusHistory(
    payment.getId(),
    "FAILED",
    "Payment failed: Insufficient funds",
    "PAYMENT_GATEWAY"
);
historyRepository.save(history);
```

### Pattern 4: Audit Trail
```java
List<PaymentStatusHistory> auditTrail = historyRepository
    .findByPaymentIdOrderByChangedAtDesc(paymentId);

// Displays full lifecycle of payment
auditTrail.forEach(h -> 
    System.out.println(h.getChangedAt() + " -> " + h.getStatus() + " (" + h.getRemarks() + ")")
);
```

---

## Dependencies

The following dependencies are required (automatically managed by Maven):
- `org.springframework.boot:spring-boot-starter-data-jpa`
- `org.springframework.boot:spring-boot-starter-web`
- `com.mysql:mysql-connector-j:8.2.0`

---

## MySQL Connection

### Docker Configuration
The application expects MySQL to be available at:
- **URL**: `jdbc:mysql://localhost:3306/rupeex?useSSL=false&serverTimezone=UTC`
- **Username**: `root`
- **Password**: (empty or as configured)

These can be overridden via environment variables:
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

### Sample docker-compose Entry
```yaml
mysql:
  image: mysql:8.0
  environment:
    MYSQL_ROOT_PASSWORD: root_password
    MYSQL_DATABASE: rupeex
  ports:
    - "3306:3306"
```

---

## Best Practices

1. **Idempotency**: Always use unique idempotency keys to prevent duplicate payments
2. **Audit Trail**: Always record status changes in PaymentStatusHistory
3. **Account Validation**: Verify accounts exist before creating payments
4. **Error Handling**: Populate error_code when payment fails
5. **Transactions**: Use @Transactional for multi-step payment operations
6. **Indexes**: Queries on status, created_at, and account_number will be fast
7. **Metadata**: Use JSON in accounts.metadata for extensibility

---

## Next Steps

1. Create Service classes to encapsulate business logic
2. Create Controller endpoints for payment operations
3. Add validation annotations (@NotNull, @Size, etc.)
4. Implement exception handling
5. Add unit tests using `@DataJpaTest`
6. Create DTO classes for API requests/responses

