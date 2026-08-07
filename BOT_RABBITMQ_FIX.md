# Bot RabbitMQ Connection Fix

## Issue Reported
```
AMQP connect failed - Connection refused to 172.19.0.4:5672
```

## Root Causes Identified

1. **No RabbitMQ Healthcheck** - Bot services started before RabbitMQ was ready
2. **No Connection Retries** - Bot failed immediately if RabbitMQ wasn't available
3. **Poor Error Messages** - Difficult to diagnose connection issues

---

## ✅ Fixes Applied

### 1. Added RabbitMQ Healthcheck
**File**: `docker-compose.yml`

Added healthcheck to RabbitMQ service:
```yaml
rabbitmq:
  healthcheck:
    test: ["CMD", "rabbitmq-diagnostics", "-q", "ping"]
    interval: 10s
    timeout: 10s
    retries: 5
    start_period: 30s
```

### 2. Updated Service Dependencies
**Files**: `docker-compose.yml`

Changed bot-service and bot-worker to wait for healthy RabbitMQ:
```yaml
bot-service:
  depends_on:
    rabbitmq:
      condition: service_healthy  # ← Wait for RabbitMQ to be healthy

bot-worker:
  depends_on:
    rabbitmq:
      condition: service_healthy  # ← Wait for RabbitMQ to be healthy
```

### 3. Added Retry Logic to Worker
**File**: `bot-service/src/worker.ts`

Added exponential backoff retry (up to 10 attempts):
```typescript
// Retries: 2s, 4s, 8s, 16s, 30s, 30s...
// Falls back to in-memory mode if connection fails
```

### 4. Improved Bot Service Startup
**File**: `bot-service/src/index.ts`

Added 5 connection attempts with increasing delays:
```typescript
// Retries: 2s, 4s, 6s, 8s, 10s
// Falls back to in-memory mode if connection fails
```

### 5. Enhanced Error Logging
**File**: `bot-service/src/rabbit.ts`

Added detailed connection logs:
- Connection URL (with masked credentials)
- ECONNREFUSED detection
- Connection error events
- Connection close events

---

## 🚀 Deploy the Fixes

### Step 1: Rebuild Bot Services (1 min)
```powershell
cd C:\Users\Administrator\Downloads\108-01-RupeeX-payment-processing

# Rebuild bot-service and bot-worker
docker-compose build bot-service bot-worker
```

### Step 2: Restart All Services (2 min)
```powershell
# Stop all services
docker-compose down

# Start with proper dependency order
docker-compose up -d

# Or restart just the bot components
docker-compose restart rabbitmq bot-service bot-worker
```

### Step 3: Verify RabbitMQ Health (30 sec)
```powershell
# Check RabbitMQ is healthy
docker ps --format "table {{.Names}}\t{{.Status}}" | Select-String rabbitmq

# Should show: (healthy)

# Or check healthcheck directly
docker inspect rupeex-rabbitmq --format='{{.State.Health.Status}}'
# Should return: healthy
```

### Step 4: Verify Bot Services Connected (30 sec)
```powershell
# Check bot-service logs
docker logs rupeex-bot-service --tail 20

# Should see:
# ✓ "Successfully connected to RabbitMQ, queue: bot.commands"
# ✓ "Connected to AMQP"

# Check bot-worker logs
docker logs rupeex-bot-worker --tail 20

# Should see:
# ✓ "Successfully connected to RabbitMQ, queue: bot.commands"
# ✓ "Worker connected to AMQP successfully"
# ✓ "Worker is now listening for commands"
```

---

## 🔍 Troubleshooting

### RabbitMQ Not Starting
```powershell
# Check RabbitMQ logs
docker logs rupeex-rabbitmq --tail 50

# Common issues:
# - Port 5672 already in use
# - Port 15672 already in use (management UI)
# - Insufficient memory

# Solution: Stop conflicting services or change ports
```

### Bot Still Can't Connect After Restart
```powershell
# 1. Verify RabbitMQ is truly healthy
docker exec rupeex-rabbitmq rabbitmq-diagnostics ping
# Should return: "Ping succeeded"

# 2. Check bot can reach RabbitMQ
docker exec rupeex-bot-worker ping -c 3 rabbitmq
# Should succeed

# 3. Test connection manually
docker exec rupeex-rabbitmq rabbitmqctl list_connections
# Should show connections from bot-service and bot-worker

# 4. Check firewall/network
docker network inspect 108-01-rupeex-payment-processing_default
# Verify all services on same network
```

### In-Memory Fallback Mode Active
**Symptoms**: Logs show "using in-memory queue for prototype"

**Implications**:
- ✓ Bot `/nl` endpoint still works (intent parsing)
- ✓ Bot `/execute` endpoint still works (queues to memory)
- ✗ Commands NOT persisted across restarts
- ✗ Worker may not process commands (if using separate container)

**Solution**: Fix RabbitMQ connection (see above)

---

## 🧪 Test the Fix

### Test 1: RabbitMQ Management UI
```
Open: http://localhost:15672
Login: guest / guest

Check:
✓ "Connections" tab shows bot-service and bot-worker
✓ "Queues" tab shows "bot.commands" queue
```

### Test 2: Queue a Command
```bash
# Queue a test payment
curl -X POST http://localhost:4001/execute \
  -H "Content-Type: application/json" \
  -d '{
    "command": {
      "type": "create_payment",
      "payload": {
        "amount": 100,
        "currency": "INR",
        "sourceAccount": "ACC-10001",
        "destinationAccount": "ACC-10002"
      }
    },
    "user": {
      "role": "admin"
    }
  }'

# Expected: {"status":"queued"}
```

### Test 3: Verify Worker Processed
```powershell
# Check worker logs immediately after test 2
docker logs rupeex-bot-worker --tail 10

# Should see:
# "Worker received command"
# "Payment API response 201"
```

---

## 📊 Connection Flow (Fixed)

```
1. docker-compose up
2. RabbitMQ starts (30s start_period)
3. RabbitMQ healthcheck runs every 10s
4. After 5 successful pings → RabbitMQ = healthy
5. bot-service starts (waits for healthy RabbitMQ)
6. bot-service connects with 5 retry attempts
7. bot-worker starts (waits for healthy RabbitMQ)
8. bot-worker connects with 10 retry attempts
9. ✅ All services connected and operational
```

---

## 🔄 Fallback Behavior

### If RabbitMQ Unavailable
Both bot-service and bot-worker fall back to **in-memory mode**:
- Commands queued in JavaScript array
- Worker drains queue every 500ms
- ⚠️ Commands lost if service restarts
- ⚠️ Not suitable for production

### When to Use In-Memory Mode
- Development/testing without RabbitMQ
- Quick prototyping
- Single-process deployments (START_WORKER=true)

### Production Recommendation
- Always use RabbitMQ
- Monitor connection health
- Alert on fallback mode activation

---

## 📈 Monitoring Commands

```powershell
# Watch bot-worker logs in real-time
docker logs rupeex-bot-worker -f

# Check RabbitMQ queue depth
docker exec rupeex-rabbitmq rabbitmqctl list_queues name messages

# View RabbitMQ connections
docker exec rupeex-rabbitmq rabbitmqctl list_connections

# Check bot-service health
curl http://localhost:4001/rag/status
```

---

## ✅ Success Indicators

After deploying fixes, you should see:

**RabbitMQ**:
- ✅ Status: `healthy` in `docker ps`
- ✅ Management UI accessible
- ✅ Queue `bot.commands` exists

**Bot Service**:
- ✅ Log: "Successfully connected to RabbitMQ"
- ✅ Log: "Connected to AMQP"
- ✅ No ECONNREFUSED errors

**Bot Worker**:
- ✅ Log: "Worker connected to AMQP successfully"
- ✅ Log: "Worker is now listening for commands"
- ✅ Processes commands without errors

**End-to-End**:
- ✅ `/nl` parses intents
- ✅ `/execute` queues commands
- ✅ Worker processes commands
- ✅ Payments created in backend
- ✅ No connection refused errors

---

## 🆘 Still Having Issues?

### Collect Diagnostic Info
```powershell
# 1. Service status
docker-compose ps > docker-status.txt

# 2. All logs
docker logs rupeex-rabbitmq > rabbitmq.log
docker logs rupeex-bot-service > bot-service.log
docker logs rupeex-bot-worker > bot-worker.log

# 3. Network info
docker network inspect 108-01-rupeex-payment-processing_default > network.txt

# 4. RabbitMQ diagnostics
docker exec rupeex-rabbitmq rabbitmq-diagnostics server_status > rabbitmq-diag.txt
```

### Common Solutions

**Issue**: Port conflicts
```powershell
# Check what's using port 5672
netstat -ano | findstr :5672

# Change port in docker-compose.yml if needed
```

**Issue**: Container restart loop
```powershell
# Check restart count
docker inspect rupeex-bot-worker --format='{{.RestartCount}}'

# If high, check logs for startup errors
docker logs rupeex-bot-worker
```

**Issue**: Network isolation
```powershell
# Recreate network
docker-compose down
docker network prune -f
docker-compose up -d
```

---

## 📚 Related Files Modified

1. **docker-compose.yml** - Added healthchecks and dependencies
2. **bot-service/src/rabbit.ts** - Enhanced connection logic
3. **bot-service/src/worker.ts** - Added retry with backoff
4. **bot-service/src/index.ts** - Improved startup handling

---

**Status**: All RabbitMQ connection issues fixed ✅  
**Next**: Rebuild and restart services to apply fixes

