# Bot Payment Creation Troubleshooting Guide

## Issues Identified and Fixed

### 1. ✅ FIXED: Missing destinationCountry Field
**Issue**: The `PaymentOrchestrationService.createPayment()` method was fetching `destinationCountry` from accounts but never setting it on the payment entity.

**Location**: `backend/src/main/java/com/rupeex/main/platform/service/PaymentOrchestrationService.java:121-122`

**Fix Applied**: Added `payment.setDestinationCountry(destinationCountry);` after line 121.

---

## Potential Blockers to Check

### 2. Access Control Issues

The bot enforces strict access control based on the `BotUser` object:

**Check these scenarios:**

#### Scenario A: User has no account number
```typescript
// If user object is like this:
{ customerId: "123", name: "John", role: "member" }
// But missing: accountNumber: "ACC-10001"
```

**Error**: `"No account is associated with the current user."`

**Solution**: Ensure the `user` object passed to `/nl` endpoint includes:
```json
{
  "customerId": "123",
  "name": "John Doe",
  "accountNumber": "ACC-10001",
  "role": "member"
}
```

#### Scenario B: User tries to create payment from different account
```typescript
// User account: ACC-10001
// Trying to create payment from: ACC-10002
```

**Error**: `"You can only access your own account."`

**Solution**: 
- Use admin role: `"role": "admin"` (no restrictions)
- Or ensure sourceAccount matches user's accountNumber

#### Scenario C: Missing user object entirely
```typescript
// Request to /nl endpoint:
{ "text": "create payment 500 INR from ACC-10001 to ACC-10002" }
// Missing: "user": {...}
```

**Fix**: Always include user object in bot requests.

---

### 3. Backend Service Connectivity

**Check if bot can reach backend:**

```powershell
# From bot-service container or host:
curl http://app:8080/api/accounts
# Or from host machine:
curl http://localhost:8082/api/accounts
```

**Expected**: List of accounts (may be empty initially)
**If fails**: Check docker-compose networking, ensure `app` service is running

---

### 4. Required Accounts Must Exist

The fraud detection engine tries to fetch account information. While payment creation won't fail if accounts don't exist, it's recommended to create them first:

**Create test accounts:**

```bash
# Using the backend API directly
curl -X POST http://localhost:8082/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "ACC-10001",
    "accountHolder": "John Doe",
    "accountType": "SAVINGS",
    "currency": "INR",
    "countryCode": "IN",
    "email": "john@example.com"
  }'

curl -X POST http://localhost:8082/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "ACC-10002",
    "accountHolder": "Jane Doe",
    "accountType": "SAVINGS",
    "currency": "INR",
    "countryCode": "IN",
    "email": "jane@example.com"
  }'
```

---

### 5. RabbitMQ Queue Issues

The bot publishes commands to RabbitMQ, and the worker consumes them.

**Check RabbitMQ:**
- Management UI: http://localhost:15672 (guest/guest)
- Verify queue `payment_commands` exists
- Check for messages in the queue

**Check worker logs:**
```powershell
docker logs rupeex-bot-worker -f
```

**Common issues:**
- Worker not started: Set `START_WORKER=true` in bot-service env
- Connection failed: Ensure RabbitMQ service is healthy
- Command stuck in queue: Worker may have crashed, check logs

---

### 6. Validation Requirements

The bot must send ALL required fields to the backend:

**Required fields** (from `PaymentPlatformRequest.java`):
- ✅ `amount` (BigDecimal, > 0.01)
- ✅ `currency` (String, not blank)
- ✅ `sourceAccount` (String, not blank)
- ✅ `destinationAccount` (String, not blank)
- ✅ `idempotencyKey` (String, not blank, auto-generated if missing)

**Optional fields** (auto-fetched from accounts if missing):
- `originCountry` (defaults to 'IN' in bot worker)
- `destinationCountry` (defaults to 'IN' in bot worker)
- `payerEmail`

---

## Testing the Bot Payment Flow

### Step 1: Start all services
```powershell
cd C:\Users\Administrator\Downloads\108-01-RupeeX-payment-processing
docker-compose up -d
```

### Step 2: Create test accounts
Run the curl commands from section 4 above, or use the frontend at http://localhost:8081

### Step 3: Test bot payment creation

**Option A: Using curl directly**
```bash
# 1. Parse intent
curl -X POST http://localhost:4001/nl \
  -H "Content-Type: application/json" \
  -d '{
    "text": "create payment 500 INR from ACC-10001 to ACC-10002",
    "user": {
      "customerId": "1",
      "name": "John Doe",
      "accountNumber": "ACC-10001",
      "role": "member"
    }
  }'

# Response should include intent with type: "create_payment"

# 2. Execute or confirm the command
curl -X POST http://localhost:4001/execute \
  -H "Content-Type: application/json" \
  -d '{
    "command": {
      "type": "create_payment",
      "payload": {
        "amount": 500,
        "currency": "INR",
        "sourceAccount": "ACC-10001",
        "destinationAccount": "ACC-10002",
        "idempotencyKey": "test-001"
      }
    },
    "user": {
      "customerId": "1",
      "name": "John Doe",
      "accountNumber": "ACC-10001",
      "role": "member"
    }
  }'

# Response should be: {"status": "queued"}
```

**Option B: As admin (no restrictions)**
```bash
curl -X POST http://localhost:4001/nl \
  -H "Content-Type: application/json" \
  -d '{
    "text": "create payment 500 INR from ACC-10001 to ACC-10002",
    "user": {
      "customerId": "admin",
      "name": "Admin User",
      "role": "admin"
    }
  }'
```

### Step 4: Check worker logs
```powershell
docker logs rupeex-bot-worker -f
```

Look for:
- `Worker received command`
- `Payment API response`
- Any error messages

### Step 5: Verify payment created
```bash
# List all payments
curl http://localhost:8082/api/payments/all

# Or check specific payment by ID
curl http://localhost:8082/api/payments/1
```

---

## Common Error Messages and Solutions

| Error | Cause | Solution |
|-------|-------|----------|
| "No account is associated with the current user" | User object missing accountNumber | Add accountNumber to user object |
| "You can only access your own account" | Member trying to use different account | Use admin role or correct accountNumber |
| "That's not your account, so I can't help" | Access control blocking at intent parsing | Check sourceAccount matches user accountNumber |
| "sourceAccount and destinationAccount are both required" | Missing account info in payload | Ensure intent parser extracts both accounts |
| "amount must be a positive number" | Amount is 0, negative, or undefined | Check intent parser extracts amount correctly |
| "Duplicate idempotency key" | Same idempotency key used twice | Use unique keys or let bot auto-generate |
| Connection refused | Services not running | Run `docker-compose up -d` |
| 404 /api/payments | Wrong endpoint URL | Ensure using /api prefix |

---

## Debug Checklist

- [ ] All Docker containers running (`docker ps`)
- [ ] Backend healthy: `curl http://localhost:8082/api/accounts`
- [ ] Bot service healthy: `curl http://localhost:4001/rag/status`
- [ ] RabbitMQ accessible: http://localhost:15672
- [ ] Test accounts exist in database
- [ ] User object includes accountNumber (for members)
- [ ] User accountNumber matches sourceAccount (for members)
- [ ] Worker logs showing command received
- [ ] Backend logs for any exceptions
- [ ] Payment created successfully in database

---

## Backend Code Fix Applied

**File**: `backend/src/main/java/com/rupeex/main/platform/service/PaymentOrchestrationService.java`

**Change**: Added missing `payment.setDestinationCountry(destinationCountry);` at line 122

```java
// Before:
payment.setOriginCountry(originCountry);
payment.setScheduledAt(request.getScheduledAt());

// After:
payment.setOriginCountry(originCountry);
payment.setDestinationCountry(destinationCountry);  // ← ADDED
payment.setScheduledAt(request.getScheduledAt());
```

This ensures the destination country (fetched from the destination account or provided in request) is persisted correctly.

---

## Next Steps

1. **Rebuild backend** after the code fix:
   ```powershell
   docker-compose build app
   docker-compose up -d app
   ```

2. **Test the fixed flow** using the steps above

3. **Check logs** if still failing:
   ```powershell
   docker logs rupeex-app -f
   docker logs rupeex-bot-worker -f
   ```

4. **Report specific error messages** if issues persist - include:
   - Exact bot command text
   - User object sent
   - Error message from logs
   - HTTP response codes

