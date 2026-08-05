# Payment Lifecycle & Statuses

RupeeX payments move through a deterministic pipeline of statuses:
CREATED -> VALIDATED -> RISK_ANALYZED -> FRAUD_CHECKED -> QUEUED -> PROCESSING -> SENT -> SETTLED.

Terminal failure states are FAILED and CANCELLED. A payment in FAILED state that
has exceeded its retry limit moves to the Dead Letter Queue (DLQ) and requires
manual review or a retry action.

Each payment has: id, amount, currency, sourceAccount, destinationAccount,
originCountry, destinationCountry, an idempotency key, a riskScore (score,
category, decision, explanation), and zero or more fraudResults (rule
triggered, contribution, reason).

# Available Bot Actions

- create_payment: requires amount, currency, sourceAccount, destinationAccount.
- retry_payment: requires paymentId. Only valid for payments in FAILED state.
- cancel_payment: requires paymentId. Only valid for payments not yet SETTLED.
- query_payments: filters by status, account, or date range.

# High-Value Confirmation Rule

Any create_payment with an amount at or above the configured high-value
threshold (BOT_HIGH_VALUE_THRESHOLD, default 100000) requires explicit human
confirmation via the /confirm endpoint before it is queued for execution.
