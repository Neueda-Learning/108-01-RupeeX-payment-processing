# Fraud Rule Types

RupeeX supports the following fraud rule types, each with a configurable
threshold and risk score contribution:

- LARGE_TRANSACTION: triggers when payment amount exceeds a threshold.
- NIGHT_TRANSACTION: triggers for payments processed during night hours.
- VELOCITY_CHECK: triggers when too many payments occur in a short window.
- REPEATED_FAILED_ATTEMPTS: triggers after repeated failed payment attempts
  from the same source account.
- BLACKLISTED_ACCOUNT: triggers when either account is on a blacklist.
- HIGH_RISK_COUNTRY: triggers when origin or destination country is in the
  FATF/OFAC high-risk country list.
- NEW_ACCOUNT: triggers for accounts created recently.
- SUSPICIOUS_FREQUENCY: triggers for unusual transaction frequency patterns.

# Roles & Access

There are two roles: admin and member. Admins can view and manage all
payments, fraud rules, events, and the DLQ. Members can only view and create
payments for their own account. The bot must never bypass these access
controls: every bot-issued command carries the acting user's role and is
subject to the same authorization checks as the UI/API.
