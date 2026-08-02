# SMTP Mailer Demo Plan (Minimal + Generic)

## Goal
Create a basic and generic email notification service in this Spring Boot project that can be called directly from APIs and runs asynchronously.

This demo implementation focuses on:
- Fast setup
- Simple structure
- Easy verification

Not included for now:
- Retry logic
- Dead letter queues
- Event/listener architecture
- Advanced logging/auditing

---

## Current Implementation (What Is Already Built)

Implemented files:
- `src/main/java/com/rupeex/main/config/AsyncConfig.java`
- `src/main/java/com/rupeex/main/notification/NotificationService.java`
- `src/main/java/com/rupeex/main/notification/model/NotificationRequest.java`
- `src/main/java/com/rupeex/main/notification/template/TestEmailTemplateBuilder.java`
- `src/main/java/com/rupeex/main/notification/EmailNotificationService.java`
- `src/main/java/com/rupeex/main/notification/NotificationController.java`

Dependency added:
- `org.springframework.boot:spring-boot-starter-mail`

---

## Why `AsyncConfig.java` Looks Empty

`AsyncConfig.java` is intentionally minimal:

```java
@Configuration
@EnableAsync
public class AsyncConfig {
}
```

Why this is enough:
- `@EnableAsync` activates Spring's async method processing globally.
- Once enabled, any Spring bean method annotated with `@Async` can run in a background thread.
- In this project, `EmailNotificationService.sendNotification(...)` uses `@Async`, so API threads are not blocked by SMTP send time.

So the class has no methods yet because we do not need custom executor tuning for this demo.

---

## Minimal Flow (Direct API Call)

1. API receives notification request.
2. Controller calls `notificationService.sendNotification(request)`.
3. Service method is `@Async`, so email send runs in background.
4. API returns immediately with `202 Accepted`.

---

## API Contract

### Endpoint
- `POST /api/notifications/test`

### Request Body
`NotificationRequest` fields:
- `toEmail` (required)
- `subject` (optional; defaults to `[RupeeX Demo] Email Service Test`)
- `recipientName` (optional)
- `message` (optional)
- `referenceId` (optional)

Example payload:

```json
{
  "toEmail": "user@example.com",
  "subject": "[RupeeX Demo] Email Service Test",
  "recipientName": "Demo User",
  "message": "Testing SMTP from RupeeX",
  "referenceId": "PAY-12345"
}
```

### Response Behavior
- `400 Bad Request` if `toEmail` is empty/missing
- `202 Accepted` when request is accepted for async processing

---

## SMTP Configuration (Gmail)

Use a Gmail App Password.

In `.env`:
- `MAIL_HOST=smtp.gmail.com`
- `MAIL_PORT=587`
- `MAIL_USERNAME=<your-gmail>`
- `MAIL_PASSWORD=<your-gmail-app-password>`
- `MAIL_SMTP_AUTH=true`
- `MAIL_SMTP_STARTTLS=true`
- `MAIL_FROM=<your-gmail>`

In `src/main/resources/application.properties`:
- `spring.mail.host=${MAIL_HOST:localhost}`
- `spring.mail.port=${MAIL_PORT:587}`
- `spring.mail.username=${MAIL_USERNAME:}`
- `spring.mail.password=${MAIL_PASSWORD:}`
- `spring.mail.properties.mail.smtp.auth=${MAIL_SMTP_AUTH:false}`
- `spring.mail.properties.mail.smtp.starttls.enable=${MAIL_SMTP_STARTTLS:false}`
- `notification.mail.from=${MAIL_FROM:no-reply@rupeex.local}`

---

## One Test Template (Current Behavior)

`TestEmailTemplateBuilder` creates a plain-text body with:
- Greeting (`recipientName`, fallback `User`)
- Message (`message`, fallback `Your notification service is working.`)
- `referenceId` line (only if provided)
- Current timestamp

---

## Test Commands

### Build Check
```powershell
Set-Location "C:\Users\Administrator\Desktop\RupeeX-payment-processing"
.\mvnw.cmd -q -DskipTests compile
```

### Call Endpoint (PowerShell)
```powershell
curl.exe -X POST "http://localhost:8080/api/notifications/test" -H "Content-Type: application/json" -d "{\"toEmail\":\"user@example.com\",\"subject\":\"[RupeeX Demo] Email Service Test\",\"recipientName\":\"Demo User\",\"message\":\"Testing SMTP from RupeeX\",\"referenceId\":\"PAY-12345\"}"
```

---

## Troubleshooting (Demo)

- If endpoint returns `400`, check `toEmail` in request body.
- If endpoint returns `202` but no mail arrives, check:
  - Gmail App Password value
  - `MAIL_FROM` and `MAIL_USERNAME`
  - Spam/Promotions folder
- If IDE shows unresolved mail imports but Maven compile passes, reload Maven project/index in IDE.

---

## Next Integration Step

In payment status update API/service, call `notificationService.sendNotification(...)` directly with appropriate `NotificationRequest` values for approved/cancelled/success/2FA-required scenarios.
