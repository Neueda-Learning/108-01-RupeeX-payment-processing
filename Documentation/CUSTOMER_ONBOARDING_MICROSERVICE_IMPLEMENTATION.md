# Customer Onboarding Microservice - Implementation Plan (No KYC, No Migrations)

## Why this document
This is the execution guide to build a new `customer-onboarding` microservice now, keep payment development moving, and later clean up duplicated onboarding/account logic in payment service.

This version is updated with your decisions:
- No KYC in phase 1.
- No Flyway/migration phase for now.
- Follow the same style/pattern as current payment backend and run as an independent container.

---

## 1) Final approach
- Build a separate onboarding service now.
- Keep payment service unchanged during initial onboarding build.
- Integrate payment with onboarding later through simple API contract.
- After integration is stable, remove duplicate onboarding/account logic from payment service.

---

## 2) Scope and boundaries

### In scope
- New onboarding microservice (Spring Boot + Maven + MySQL).
- Customer profile onboarding.
- Consent capture.
- Onboarding status workflow.
- REST APIs.
- Docker + CI flow aligned with current payment service pattern.
- Later integration path for payment service.

### Out of scope
- Frontend.
- KYC (deferred to future phase).
- DB migration tooling (Flyway/Liquibase) for now.

### Ownership boundaries
- Onboarding service owns customer onboarding lifecycle and status.
- Payment service owns payment lifecycle only.
- Payment service consumes onboarding status (`APPROVED`, `REJECTED`, `PENDING_REVIEW`).

---

## 3) Target repository layout (same pattern style)
Use same style as current repository and keep onboarding in separate folder.

```text
RupeeX-payment-processing/
  src/                                  # existing payment service
  onboarding-service/                   # new service
    pom.xml
    src/main/java/com/rupeex/onboarding/
      OnboardingApplication.java
      controller/
      dto/
      entity/
      enums/
      exception/
      repository/
      service/
      service/impl/
      config/
    src/main/resources/
      application.properties
      schema.sql
    src/test/java/com/rupeex/onboarding/
    Dockerfile
    Jenkinsfile
    README.md
  Documentation/
    CUSTOMER_ONBOARDING_MICROSERVICE_IMPLEMENTATION.md
```

---

## 4) Domain model (v1, without KYC)

### Core entities
1. `Customer`
   - `id (UUID)`
   - `externalRef` (optional)
   - `fullName`, `email`, `phone`, `dob`
   - `status` (`DRAFT`, `PENDING_REVIEW`, `APPROVED`, `REJECTED`, `SUSPENDED`)
   - `createdAt`, `updatedAt`

2. `CustomerAddress`
   - `id`, `customerId`, `type` (`HOME`, `WORK`)
   - address fields

3. `Consent`
   - `id`, `customerId`
   - `consentType`, `version`, `accepted`, `acceptedAt`

4. `OnboardingAuditLog`
   - `id`, `customerId`, `eventType`
   - `oldValue`, `newValue`, `actor`, `createdAt`

5. `OutboxEvent`
   - `id`, `aggregateType`, `aggregateId`
   - `eventType`, `payloadJson`
   - `status` (`NEW`, `PUBLISHED`, `FAILED`)
   - `createdAt`, `publishedAt`

---

## 5) API contracts (v1)
All errors should follow one consistent error envelope.

### Create customer
- `POST /onboarding/customers`
- Header: `Idempotency-Key: <uuid>`
- Request:
```json
{
  "fullName": "Rahul Sharma",
  "email": "rahul@example.com",
  "phone": "+919876543210",
  "dob": "1996-04-12"
}
```
- Response: `201 Created`
```json
{
  "customerId": "0f7c1d79-7f76-4c59-9c2d-229f90c27a18",
  "status": "DRAFT"
}
```

### Get customer summary
- `GET /onboarding/customers/{customerId}`

### Record consent
- `POST /onboarding/customers/{customerId}/consents`
```json
{
  "consentType": "TERMS_AND_PRIVACY",
  "version": "v1.0",
  "accepted": true
}
```

### Submit onboarding for review
- `POST /onboarding/customers/{customerId}/submit`

### Approve onboarding
- `POST /onboarding/customers/{customerId}/approve`

### Reject onboarding
- `POST /onboarding/customers/{customerId}/reject`
```json
{
  "reason": "Document mismatch"
}
```

### Payment-facing status endpoint
- `GET /onboarding/customers/{customerId}/status`
- Example:
```json
{
  "customerId": "0f7c1d79-7f76-4c59-9c2d-229f90c27a18",
  "status": "APPROVED",
  "eligibleForPayments": true
}
```

---

## 6) State machine rules
- `DRAFT -> PENDING_REVIEW`
- `PENDING_REVIEW -> APPROVED`
- `PENDING_REVIEW -> REJECTED`
- `APPROVED -> SUSPENDED` (future admin action)
- Any other transition returns `409 CONFLICT`

Mandatory validation before `submit`:
- Required customer profile fields present.
- At least one accepted consent.

---

## 7) Database approach (same as current repo pattern)
- Keep `schema.sql` under `onboarding-service/src/main/resources/schema.sql`.
- Use `spring.jpa.hibernate.ddl-auto=update` in onboarding `application.properties` (same style as payment service).
- No migration scripts in phase 1.
- Since no production data exists, schema reset in development is acceptable.

---

## 8) Service implementation checklist (same code pattern)

### 8.1 Bootstrapping
Create `onboarding-service/pom.xml` with dependencies matching current backend style:
- `spring-boot-starter`
- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-validation`
- `spring-boot-starter-test`
- `mysql-connector-j`
- `lombok` (if needed)

### 8.2 Packages
Use same package layering pattern as existing backend:
- `controller`
- `dto`
- `entity`
- `enums`
- `exception`
- `repository`
- `service`
- `service/impl`
- `config`

### 8.3 Controllers
- `CustomerOnboardingController`
- Implement endpoints from section 5.
- Validate input with `jakarta.validation`.

### 8.4 Services
- `CustomerOnboardingService`
- `ConsentService`
- `EligibilityService`
- Keep transition validation logic in service layer.

### 8.5 Repositories
- JPA repositories for all entities.
- Useful methods:
  - `findByEmail(...)`
  - `findByPhone(...)`
  - `findByCustomerId(...)`
  - `existsBy...`

### 8.6 Error handling
- Add `GlobalExceptionHandler` like existing backend style.
- Standard error response:
```json
{
  "errorCode": "VALIDATION_FAILED",
  "message": "Full name is required",
  "timestamp": "2026-07-31T14:25:00Z",
  "traceId": "..."
}
```

### 8.7 Outbox events (optional now, recommended)
- On status changes, insert one row in `outbox_events`.
- Add scheduled publisher job later when event broker is introduced.

---

## 9) Payment service integration plan (later)

### Phase A
- Build onboarding service independently.
- No payment runtime dependency.

### Phase B
- Add payment feature flag:
  - `feature.payment.onboardingCheck.enabled=false`
- If enabled, payment service calls onboarding status endpoint before accepting payment.

### Phase C (optional)
- Introduce event-driven cache in payment service.
- Keep API fallback for cache misses.

---

## 10) Payment-service cleanup plan (after integration)
1. Remove onboarding-specific checks from payment validation.
2. Keep only payment rules in payment service.
3. Consolidate duplicate payment domain classes:
   - `entity.Payment` vs `model.Payments`
   - `entity.PaymentHistory` vs `model.PaymentStatusHistory`
4. Consolidate duplicate repositories:
   - `PaymentRepository` and `PaymentsRepository`
5. Remove placeholders/commented dead code.

Exit criteria:
- Onboarding API stable.
- Payment integration tests green.
- No active path depending on old duplicate onboarding logic.

---

## 11) Testing strategy

### Unit tests
- Transition rules.
- Validation rules.
- Idempotency behavior.

### Integration tests
- Repository + DB tests.
- Controller tests for full request/response flows.

### Contract tests
- Payment-facing `GET /onboarding/customers/{id}/status`.

---

## 12) CI/CD and container plan (same operational pattern)

### Docker pattern
Use same style as current service:
- Multi-stage `Dockerfile` using Maven build and JRE runtime.
- Environment via `.env`.
- Dedicated service in compose (example `onboarding-app`) with its own DB or schema.

### Jenkins pattern
Use onboarding `Jenkinsfile` similar to current flow:
1. Checkout
2. Build/test (`mvn clean test`)
3. Docker build
4. `docker compose up -d`
5. Smoke logs/health check

### Secrets
- Keep credentials in Jenkins credentials/env.
- No secrets in repository.

---

## 13) Security baseline (without KYC)
- Input validation on all fields.
- Mask email/phone in logs where possible.
- Add authn/authz before production use.
- Audit all onboarding status changes.
- Record consent version + acceptance timestamp.

---

## 14) Observability baseline
- Structured logs with `traceId`, `customerId`.
- Add health endpoint and basic metrics.
- Track:
  - onboarding started
  - approved
  - rejected
  - average onboarding duration

---

## 15) Risk and rollback

### Risks
- Duplicate logic survives too long.
- Validation differences between onboarding and payment.

### Mitigations
- Time-box cleanup with a sprint deadline.
- Add payment-onboarding contract tests.

### Rollback
- Disable payment integration flag.
- Keep onboarding service isolated.
- Revert deployment image if required.

---

## 16) Definition of done
- Onboarding service runs locally and in container.
- Core APIs implemented and documented.
- `schema.sql` + JPA update strategy working.
- Unit and integration tests green.
- Health/metrics/logging baseline enabled.
- Payment integration path feature-flagged and documented.
- Cleanup backlog for payment duplicates scheduled with owners/dates.

---

## 17) Sprint checklist

### Sprint 1 - Foundation
- [ ] Create `onboarding-service` skeleton.
- [ ] Add entities/repositories/schema.
- [ ] Implement create/get customer APIs.
- [ ] Add exception handling and base tests.

### Sprint 2 - Workflow
- [ ] Implement consent/submit/approve/reject APIs.
- [ ] Add state transition validations.
- [ ] Add audit logging.
- [ ] Add idempotency handling.

### Sprint 3 - Hardening
- [ ] Add status endpoint contract test for payment service.
- [ ] Add compose wiring and onboarding Docker container.
- [ ] Add onboarding Jenkins pipeline.
- [ ] Add logs/health/metrics hardening.

### Sprint 4 - Integration and cleanup
- [ ] Add feature-flagged onboarding check in payment service.
- [ ] Validate end-to-end in dev/qa.
- [ ] Remove duplicate onboarding logic from payment service.
- [ ] Consolidate duplicate payment models/repositories.

---

## 18) Exact local commands (Windows PowerShell)
Run from repo root.

### Validate toolchain
```powershell
Set-Location "C:\Users\Administrator\Desktop\RupeeX-payment-processing"
.\mvnw.cmd -v
docker --version
docker compose version
```

### Create onboarding service folder
```powershell
Set-Location "C:\Users\Administrator\Desktop\RupeeX-payment-processing"
New-Item -ItemType Directory -Path ".\onboarding-service" -Force
```

### Build current payment service baseline
```powershell
Set-Location "C:\Users\Administrator\Desktop\RupeeX-payment-processing"
.\mvnw.cmd test
```

### Build onboarding service later (after pom/code exists)
```powershell
Set-Location "C:\Users\Administrator\Desktop\RupeeX-payment-processing"
.\mvnw.cmd -f ".\onboarding-service\pom.xml" clean test
```

### Compose run (after onboarding service is added to compose)
```powershell
Set-Location "C:\Users\Administrator\Desktop\RupeeX-payment-processing"
docker compose up -d
docker compose ps
docker compose logs -f onboarding-app
```

### Non-prod reset (development)
```powershell
Set-Location "C:\Users\Administrator\Desktop\RupeeX-payment-processing"
docker compose down -v
```

---

## 19) Final implementation order
1. Create onboarding service skeleton with same project pattern as payment service.
2. Add customer/consent/status workflow APIs (no KYC).
3. Add validation, audit logs, and tests.
4. Add Docker/Jenkins wiring in same style as current backend flow.
5. Add feature-flagged payment integration.
6. Remove duplicated onboarding logic from payment service.

This keeps delivery fast, simple, and aligned with your current operational pattern.
