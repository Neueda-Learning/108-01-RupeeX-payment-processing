# Bot Payment Creation - Quick Fix Checklist

## ✅ Issues Fixed

1. **Missing destinationCountry setter** in PaymentOrchestrationService ✓
2. **Missing getters/setters** for currency exchange fields in Payment entity ✓

## 🚀 Deploy & Test (5 minutes)

### Step 1: Rebuild Backend (1 min)
```powershell
cd C:\Users\Administrator\Downloads\108-01-RupeeX-payment-processing
docker-compose build app
docker-compose up -d app
```

### Step 2: Verify Services Running (30 sec)
```powershell
docker ps --format "table {{.Names}}\t{{.Status}}"
```
**Expected**: All services running (rupeex-app, rupeex-bot-service, rupeex-bot-worker, rupeex-db, rupeex-rabbitmq)

### Step 3: Create Test Accounts (1 min)
```bash
curl -X POST http://localhost:8082/api/accounts -H "Content-Type: application/json" -d '{"accountNumber":"ACC-10001","accountHolder":"Alice Test","accountType":"SAVINGS","currency":"INR","countryCode":"IN","email":"alice@test.com"}'

curl -X POST http://localhost:8082/api/accounts -H "Content-Type: application/json" -d '{"accountNumber":"ACC-10002","accountHolder":"Bob Test","accountType":"SAVINGS","currency":"INR","countryCode":"IN","email":"bob@test.com"}'
```

### Step 4: Test Bot Payment (2 min)
```bash
# Parse intent
curl -X POST http://localhost:4001/nl -H "Content-Type: application/json" -d '{"text":"create payment 500 INR from ACC-10001 to ACC-10002","user":{"customerId":"admin","name":"Admin","role":"admin"}}'

# Execute command (use actual intent from above response)
curl -X POST http://localhost:4001/execute -H "Content-Type: application/json" -d '{"command":{"type":"create_payment","payload":{"amount":500,"currency":"INR","sourceAccount":"ACC-10001","destinationAccount":"ACC-10002"}},"user":{"customerId":"admin","name":"Admin","role":"admin"}}'
```

**Expected Response**: `{"status":"queued"}`

### Step 5: Verify Payment Created (30 sec)
```bash
# Check worker logs
docker logs rupeex-bot-worker --tail 20

# Verify payment in database
curl http://localhost:8082/api/payments/all
```

**Expected**: Payment with status CREATED/QUEUED/SETTLED visible in response

---

## 🔍 If Still Failing - Check These

### Accounts Missing
```bash
curl http://localhost:8082/api/accounts
# Should return list with ACC-10001 and ACC-10002
```

### RabbitMQ Not Connected
```powershell
docker logs rupeex-bot-worker | Select-String "AMQP"
# Should see "Connected to AMQP"
```

### User Access Control Issue
**For member users**, ensure user.accountNumber matches sourceAccount:
```json
{
  "user": {
    "accountNumber": "ACC-10001",  // Must match sourceAccount
    "role": "member"
  },
  "command": {
    "payload": {
      "sourceAccount": "ACC-10001"  // Must match user.accountNumber
    }
  }
}
```

**Or use admin role** (no restrictions):
```json
{
  "user": {
    "role": "admin"  // Can create payments from any account
  }
}
```

### Backend Not Reachable
```powershell
curl http://localhost:8082/api/actuator/health
# Should return {"status":"UP"}
```

---

## 📝 Key Files Modified

1. `PaymentOrchestrationService.java` - Line 122: Added `setDestinationCountry()`
2. `Payment.java` - Lines 319-379: Added currency exchange getters/setters

---

## 📚 Full Documentation

- **Detailed Fixes**: `BOT_PAYMENT_FIXES_SUMMARY.md`
- **Troubleshooting Guide**: `BOT_PAYMENT_TROUBLESHOOTING.md`
- **Architecture**: `ARCHITECTURE.md`

---

**Quick Test Command (PowerShell)**:
```powershell
$user = @{customerId="admin";name="Admin";role="admin"} | ConvertTo-Json -Compress
$payload = @{text="create payment 500 INR from ACC-10001 to ACC-10002";user=$user} | ConvertFrom-Json | ConvertTo-Json

Invoke-RestMethod -Uri http://localhost:4001/nl -Method Post -Body $payload -ContentType "application/json"
```

---

✅ **Status**: Ready to deploy and test

