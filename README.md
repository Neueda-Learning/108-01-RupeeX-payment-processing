# Payment Processing & Risk Intelligence Platform

Production-style banking payment processing platform implementing end-to-end lifecycle orchestration, fraud/risk intelligence, retries, dead-letter queue handling, and real-time event streaming.

## Tech Stack

- Backend: Java 21, Spring Boot 3, Spring Data JPA, Spring WebSocket, MySQL, Maven
- Frontend: Next.js 15, TypeScript, TailwindCSS, ShadCN-ready dependency baseline, React Query, Zustand, Recharts, Framer Motion, React Flow
- DevOps: Docker, Docker Compose, Jenkins
- Testing: JUnit 5, Mockito, Testcontainers
- API Docs: Swagger/OpenAPI

## Lifecycle

CREATED -> VALIDATED -> RISK_ANALYZED -> FRAUD_CHECKED -> QUEUED -> PROCESSING -> SENT -> SETTLED

Failure path:

- Any processing failure can transition to FAILED
- Retry policy drives re-queueing
- Max retry breach moves payment to dead_letter_queue

## Key APIs

- POST /payments
- GET /payments
- GET /payments/{id}
- POST /payments/{id}/retry
- POST /payments/{id}/cancel
- GET /payments/{id}/history
- GET /fraud/rules
- POST /fraud/rules
- PUT /fraud/rules/{id}
- DELETE /fraud/rules/{id}
- GET /metrics
- GET /dashboard
- GET /events
- GET /health

Swagger:

- /swagger-ui
- /api-docs

## Run Locally

### Backend

```bash
chmod +x backend/mvnw
./backend/mvnw clean package
./backend/mvnw spring-boot:run
```

### Full Stack (Docker Compose)

```bash
docker-compose up --build -d
```

## Architecture Docs

- [Platform Blueprint](Documentation/PLATFORM_ARCHITECTURE_BLUEPRINT.md)
- [Database Schema](backend/src/main/resources/schema.sql)

## Notes

- Legacy endpoints remain under /legacy/payments.
- New platform APIs are mounted at the root paths listed above.
