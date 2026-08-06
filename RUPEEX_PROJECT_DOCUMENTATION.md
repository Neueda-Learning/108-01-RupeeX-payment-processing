# RupeeX Payment Processing & Risk Intelligence Platform
## Complete Project Documentation

---

## Table of Contents
1. [Executive Summary](#executive-summary)
2. [System Architecture](#system-architecture)
3. [Technology Stack](#technology-stack)
4. [Core Features](#core-features)
5. [System Components](#system-components)
6. [Database Architecture](#database-architecture)
7. [Payment Lifecycle](#payment-lifecycle)
8. [API Architecture](#api-architecture)
9. [Frontend Architecture](#frontend-architecture)
10. [Security & Compliance](#security--compliance)
11. [Deployment Architecture](#deployment-architecture)
12. [Integration Architecture](#integration-architecture)
13. [Operational Features](#operational-features)
14. [Flow Diagrams](#flow-diagrams)

---

## 1. Executive Summary

### Project Overview
**RupeeX** is a production-grade, enterprise-level payment processing and risk intelligence platform designed for secure, scalable, and compliant financial transactions. The platform implements end-to-end payment lifecycle orchestration with built-in fraud detection, risk scoring, retry mechanisms, and real-time monitoring.

### Key Capabilities
- ✅ **Multi-currency payment processing** with real-time exchange rates
- ✅ **Intelligent fraud detection** with configurable rule engine
- ✅ **Risk scoring and analysis** for every transaction
- ✅ **Automatic retry & recovery** with dead-letter queue (DLQ)
- ✅ **Real-time event streaming** via WebSocket
- ✅ **Customer onboarding microservice** with consent management
- ✅ **OTP-based authentication** for secure transactions
- ✅ **Natural language bot interface** with on-premise AI/ML
- ✅ **Comprehensive audit trails** for compliance
- ✅ **Interactive analytics & insights** with visual dashboards
- ✅ **Scheduled payment processing** for future-dated transactions
- ✅ **Multi-platform support** with responsive web interface

### Business Value
- **Regulatory Compliance**: Complete audit trail, consent management, fraud detection
- **Operational Excellence**: Automated workflows, retry mechanisms, dead-letter queues
- **Risk Management**: Multi-layered fraud detection and risk scoring
- **Scalability**: Microservices architecture with containerized deployment
- **User Experience**: Real-time updates, natural language interface, mobile-responsive UI

---

## 2. System Architecture

### 2.1 Architecture Style
```
┌─────────────────────────────────────────────────────────────┐
│         CLEAN ARCHITECTURE WITH MODULAR ENGINES             │
│                                                             │
│  ┌───────────────────────────────────────────────────┐    │
│  │     PRESENTATION LAYER                            │    │
│  │  • REST Controllers                              │    │
│  │  • WebSocket Endpoints                           │    │
│  │  • OpenAPI/Swagger Documentation                 │    │
│  └───────────────────────────────────────────────────┘    │
│                       │                                     │
│  ┌───────────────────────────────────────────────────┐    │
│  │     APPLICATION LAYER (Orchestration)             │    │
│  │  • Payment Orchestration Service                 │    │
│  │  • State Machine Management                      │    │
│  │  • Transaction Coordination                      │    │
│  └───────────────────────────────────────────────────┘    │
│                       │                                     │
│  ┌───────────────────────────────────────────────────┐    │
│  │     DOMAIN LAYER (Business Logic)                │    │
│  │  • Validation Engine                             │    │
│  │  • Fraud Detection Engine                        │    │
│  │  • Risk Scoring Engine                           │    │
│  │  • Settlement Engine                             │    │
│  │  • Audit Engine                                  │    │
│  │  • Notification Engine                           │    │
│  │  • Metrics Engine                                │    │
│  └───────────────────────────────────────────────────┘    │
│                       │                                     │
│  ┌───────────────────────────────────────────────────┐    │
│  │     INFRASTRUCTURE LAYER                          │    │
│  │  • Spring Data JPA Repositories                  │    │
│  │  • MySQL Database                                │    │
│  │  • External API Clients                          │    │
│  │  • Message Queues (RabbitMQ)                     │    │
│  └───────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Microservices Architecture

```mermaid
graph TB
    subgraph "Frontend Layer"
        UI[Next.js 15 Frontend<br/>TypeScript + React]
    end
    
    subgraph "Gateway Layer"
        NGINX[NGINX Reverse Proxy<br/>Load Balancer]
    end
    
    subgraph "Application Services"
        PAYMENT[Payment Processing Service<br/>Spring Boot 3 + Java 21]
        ONBOARD[Customer Onboarding Service<br/>Spring Boot 3 + Java 21]
        BOT[Bot Service<br/>Node.js + TypeScript]
    end
    
    subgraph "Infrastructure Services"
        DB[(MySQL 8.0<br/>Relational Database)]
        QUEUE[RabbitMQ<br/>Message Broker]
        OLLAMA[Ollama<br/>On-Premise LLM/RAG]
    end
    
    UI --> NGINX
    NGINX --> PAYMENT
    NGINX --> ONBOARD
    NGINX --> BOT
    
    PAYMENT --> DB
    ONBOARD --> DB
    BOT --> QUEUE
    BOT --> OLLAMA
    BOT --> PAYMENT
    PAYMENT --> QUEUE
```

### 2.3 Logical Layers

| Layer | Responsibility | Technologies |
|-------|---------------|-------------|
| **Presentation** | REST APIs, WebSocket, Swagger UI | Spring Web, Spring WebSocket |
| **Application** | Use case orchestration, transaction boundaries | Spring Transaction, State Machine |
| **Domain** | Business logic engines (fraud, risk, audit) | Java Services, JPA Entities |
| **Infrastructure** | Data persistence, external integrations | Spring Data JPA, RestClient, MySQL |

---

## 3. Technology Stack

### 3.1 Backend Technologies

```
┌─────────────────────────────────────────────────────────┐
│                  BACKEND STACK                          │
├─────────────────────────────────────────────────────────┤
│ Runtime           │ Java 21 (OpenJDK Temurin)          │
│ Framework         │ Spring Boot 3.x                     │
│ Architecture      │ Spring Data JPA, Spring Web        │
│ Database          │ MySQL 8.0                          │
│ Build Tool        │ Maven 3.x                          │
│ API Documentation │ Swagger/OpenAPI 3.0                │
│ Testing           │ JUnit 5, Mockito, Testcontainers  │
│ WebSocket         │ Spring WebSocket + STOMP           │
│ Async Processing  │ Spring @Async, CompletableFuture   │
└─────────────────────────────────────────────────────────┘
```

### 3.2 Frontend Technologies

```
┌─────────────────────────────────────────────────────────┐
│                 FRONTEND STACK                          │
├─────────────────────────────────────────────────────────┤
│ Framework         │ Next.js 15 (App Router)            │
│ Language          │ TypeScript 5.x                     │
│ UI Library        │ React 19                           │
│ Styling           │ TailwindCSS 4.x                    │
│ Components        │ Radix UI, Lucide Icons             │
│ State Management  │ Zustand, React Query                │
│ Charts/Graphs     │ Recharts                           │
│ Animations        │ Framer Motion                      │
│ Flow Diagrams     │ React Flow                         │
│ WebSocket Client  │ @stomp/stompjs                     │
└─────────────────────────────────────────────────────────┘
```

### 3.3 Bot Service & AI/ML

```
┌─────────────────────────────────────────────────────────┐
│              BOT SERVICE STACK                          │
├─────────────────────────────────────────────────────────┤
│ Runtime           │ Node.js 20.x                       │
│ Language          │ TypeScript                         │
│ Message Queue     │ RabbitMQ (AMQP)                    │
│ NLP/LLM           │ Ollama (On-premise)                │
│ Language Model    │ Qwen 2.5 (0.5B parameters)         │
│ Embeddings        │ Nomic Embed Text                   │
│ RAG Framework     │ Custom in-memory vector store      │
│ HTTP Server       │ Express.js                         │
└─────────────────────────────────────────────────────────┘
```

### 3.4 DevOps & Infrastructure

```
┌─────────────────────────────────────────────────────────┐
│           DEVOPS & INFRASTRUCTURE                       │
├─────────────────────────────────────────────────────────┤
│ Containerization  │ Docker, Docker Compose             │
│ CI/CD             │ Jenkins Pipeline                   │
│ Reverse Proxy     │ NGINX 1.27                         │
│ Database          │ MySQL 8.0 (InnoDB Engine)          │
│ Message Broker    │ RabbitMQ 3.11                      │
│ Version Control   │ Git, GitHub                        │
│ Deployment        │ Multi-container orchestration      │
└─────────────────────────────────────────────────────────┘
```

---

## 4. Core Features

### 4.1 Payment Processing Features

#### ✅ Complete Payment Lifecycle Management
- **State-driven workflow**: CREATED → VALIDATED → RISK_ANALYZED → FRAUD_CHECKED → QUEUED → PROCESSING → SENT → SETTLED
- **Idempotency protection**: Prevents duplicate payment processing
- **Currency conversion**: Real-time exchange rate integration (ExchangeRate-API)
- **Multi-currency support**: INR, USD, EUR, GBP, SGD, AED, and more
- **Scheduled payments**: Future-dated transaction processing
- **Payment cancellation**: Cancel in-flight payments before processing
- **Payment retry**: Manual retry for failed transactions

#### ✅ Account Management
- **Multi-account support**: Savings, Current, Business accounts
- **Account-to-account transfers**: Internal fund transfers
- **Balance tracking**: Real-time balance updates
- **Account insights**: Transaction analytics per account
- **Email notifications**: Account activity alerts

### 4.2 Fraud Detection & Risk Intelligence

#### ✅ Dynamic Fraud Rule Engine
```
Rule Types Supported:
• AMOUNT_THRESHOLD       → High-value transaction detection
• VELOCITY_CHECK         → Rapid transaction frequency detection
• BLACKLIST              → Known suspicious accounts/entities
• CROSS_BORDER          → International transaction monitoring
• DUPLICATE_TRANSACTION  → Duplicate payment detection
• TIME_BASED            → Unusual transaction timing patterns
```

- **Configurable rules**: Create, update, delete fraud rules via API
- **Real-time evaluation**: Every payment evaluated against active rules
- **Score-based system**: Weighted contribution from each triggered rule
- **Rule explainability**: Detailed reason for each rule trigger

#### ✅ Risk Scoring System
```
Risk Categories:
• LOW        (Score: 0-30)   → Auto-approve
• MEDIUM     (Score: 31-60)  → Manual review
• HIGH       (Score: 61-100) → Auto-reject or review
```

- **Multi-factor risk analysis**: Amount, frequency, geography, account history
- **Explainable decisions**: Detailed breakdown of risk factors
- **Risk score storage**: Historical risk tracking per payment

### 4.3 Retry & Recovery Features

#### ✅ Intelligent Retry Mechanism
- **Automatic retry**: Failed payments automatically retried based on policy
- **Exponential backoff**: Configurable retry intervals
- **Max retry limit**: Prevents infinite retry loops
- **Dead Letter Queue (DLQ)**: Failed payments moved to DLQ after max retries
- **Manual DLQ recovery**: Admin can retry payments from DLQ

### 4.4 Customer Onboarding Features

#### ✅ Onboarding Microservice
- **Customer registration**: Full profile capture (name, email, phone, DOB)
- **Address management**: Multiple address support (HOME, WORK)
- **Consent management**: Terms & conditions, privacy policy tracking
- **Status workflow**: DRAFT → PENDING_REVIEW → APPROVED → REJECTED
- **Account linking**: Link customer to payment accounts
- **Audit trail**: Complete onboarding history
- **Admin review interface**: Manual approval/rejection workflow

### 4.5 Security & Authentication Features

#### ✅ OTP-Based Authentication
- **Email OTP**: 4-digit OTP sent via email (SMTP integration)
- **OTP validation**: Time-bound OTP verification
- **Payment authorization**: OTP required for payment submission
- **Fallback mode**: Console-based OTP for development/testing
- **OTP expiry**: Configurable expiration time

#### ✅ Role-Based Access Control (RBAC)
```
User Roles:
• ADMIN    → Full system access, can manage all accounts
• MEMBER   → Limited access, can only view/manage own account
```

- **Account isolation**: Members can only access their own data
- **Admin privileges**: Full platform visibility and control
- **User switcher**: Switch between multiple user personas (UI feature)

### 4.6 Audit & Compliance Features

#### ✅ Comprehensive Audit Trail
- **Payment history**: Complete state transition log per payment
- **Audit logs**: Service-level action tracking with timestamps
- **Processing metrics**: Track processing time for each stage
- **Before/after state**: Captures state changes for compliance
- **Actor tracking**: Records who performed each action

#### ✅ System Event Logging
- **Event types**: PAYMENT_CREATED, PAYMENT_COMPLETED, PAYMENT_FAILED, PAYMENT_RETRY, etc.
- **Real-time feed**: WebSocket-based event streaming to frontend
- **Searchable history**: Query past events by type, date, payment ID
- **Structured events**: JSON-based event payloads

### 4.7 Bot & Natural Language Interface

#### ✅ Conversational Bot Service
```
Supported Commands:
• "Create payment of 5000 INR to AC123456"
• "Show all payments"
• "Get payment status for REF-XYZ"
• "List failed transactions"
• "Send 1000 USD from AC111 to AC222"
```

- **Natural language processing**: Parse conversational commands
- **On-premise LLM**: Ollama-powered intent extraction (Qwen 2.5 0.5B)
- **RAG-enhanced**: Context-aware responses using knowledge base
- **Confirmation workflow**: High-value transactions require confirmation
- **Command queue**: Async execution via RabbitMQ
- **Fallback parser**: Rule-based backup when LLM unavailable

### 4.8 Analytics & Reporting Features

#### ✅ Admin Dashboard
- **Platform-wide metrics**: Total payments, volume, success rate, failure rate
- **Time-based trends**: Hour/Day/Month/Year granularity
- **Status breakdown**: Visual distribution of payment states
- **Account activity**: Per-account transaction summary
- **Recent payments**: Real-time payment feed
- **Quick links**: Navigate to rules, DLQ, events, accounts

#### ✅ Account Insights Page
- **Transaction analytics**: Sent vs received amounts over time
- **Status distribution**: Success/failure rate charts
- **Time filters**: Per hour, per day, per month analysis
- **Visual charts**: Line charts, bar charts, pie charts (Recharts)
- **Transaction history**: Tabular view with sent/received tabs

### 4.9 Operational Features

#### ✅ Queue Management
- **Processing queue**: Async payment processing scheduler
- **Priority handling**: READY → PROCESSING → COMPLETED workflow
- **Concurrent processing**: Async batch processing
- **Queue monitoring**: View queue depth and processing status

#### ✅ Dead Letter Queue (DLQ)
- **Failed payment storage**: Capture payments after retry exhaustion
- **DLQ inspection**: Admin UI to view failed payments
- **Manual recovery**: Retry payments from DLQ
- **Failure analytics**: Analyze common failure patterns

#### ✅ Notification System
- **Email notifications**: SMTP-based email delivery
- **Test endpoint**: Send test notifications for verification
- **Notification history**: Track all sent notifications
- **Template support**: Configurable notification templates

#### ✅ Real-Time Updates
- **WebSocket integration**: Live updates via STOMP protocol
- **Event subscription**: Subscribe to `/topic/events`
- **Auto-refresh**: Dashboard and lists update in real-time
- **Connection management**: Auto-reconnect on disconnect

---

## 5. System Components

### 5.1 Payment Processing Service (Backend)

```
com.rupeex.main/
├── controller/
│   ├── PaymentPlatformController.java      → Payment CRUD + lifecycle ops
│   ├── AccountController.java              → Account management
│   ├── FraudRuleController.java            → Fraud rule management
│   ├── DeadLetterQueueController.java      → DLQ operations
│   ├── PlatformMetricsController.java      → Metrics + dashboard
│   ├── PaymentAuditController.java         → Audit log queries
│   ├── ExchangeRateController.java         → Currency conversion
│   ├── NotificationController.java         → Notification testing
│   └── OtpController.java                  → OTP send/verify
│
├── platform/service/
│   ├── PaymentOrchestrationService.java    → Main orchestrator
│   ├── PaymentStateMachine.java            → State transition logic
│   ├── FraudDetectionEngineService.java    → Fraud rule evaluation
│   ├── RiskScoringEngineService.java       → Risk calculation
│   ├── QueueProcessingScheduler.java       → Async queue processor
│   ├── SettlementEngineService.java        → Payment settlement
│   ├── AuditEngineService.java             → Audit log creation
│   ├── NotificationEngineService.java      → Notification dispatch
│   ├── MetricsEngineService.java           → Metrics aggregation
│   ├── SystemEventService.java             → Event publishing
│   ├── ScheduledPaymentReleaseScheduler.java → Future payment release
│   └── SystemEventWebSocketPublisher.java  → WebSocket broadcast
│
├── entity/
│   ├── Payment.java              → Core payment entity
│   ├── Account.java              → Account entity
│   ├── PaymentHistory.java       → State transition history
│   ├── AuditLog.java             → Audit trail
│   ├── FraudRule.java            → Fraud rule definition
│   ├── FraudResult.java          → Fraud evaluation result
│   ├── RiskScore.java            → Risk scoring result
│   ├── ProcessingQueue.java      → Queue entry
│   ├── DeadLetterQueueEntry.java → DLQ entry
│   ├── NotificationRecord.java   → Notification log
│   └── SystemEvent.java          → System event log
│
├── repository/
│   └── [JpaRepository interfaces for all entities]
│
├── dto/
│   ├── PaymentPlatformRequest.java
│   ├── PaymentPlatformResponse.java
│   ├── FraudRuleRequest.java
│   ├── MetricsSnapshotResponse.java
│   ├── ExchangeRequest.java
│   └── ExchangeResponse.java
│
└── config/
    ├── AsyncConfig.java          → Async executor configuration
    └── WebSocketConfig.java      → STOMP WebSocket configuration
```

### 5.2 Customer Onboarding Service

```
com.rupeex.onboarding/
├── controller/
│   └── CustomerOnboardingController.java   → Onboarding CRUD + workflow
│
├── entity/
│   ├── Customer.java             → Customer profile
│   ├── CustomerAddress.java      → Address information
│   ├── Consent.java              → Consent records
│   ├── OnboardingAuditLog.java   → Onboarding history
│   └── OutboxEvent.java          → Event outbox pattern
│
├── service/
│   ├── CustomerOnboardingService.java      → Onboarding orchestrator
│   └── CustomerOnboardingServiceImpl.java  → Implementation
│
└── dto/
    ├── CustomerRequest.java
    ├── CustomerResponse.java
    └── ConsentRequest.java
```

### 5.3 Bot Service (Node.js/TypeScript)

```
bot-service/
├── src/
│   ├── index.ts              → HTTP server (Express)
│   ├── worker.ts             → RabbitMQ consumer
│   ├── intent.ts             → NLP intent parsing
│   ├── slm.ts                → Ollama LLM integration
│   ├── rag.ts                → RAG embedding + retrieval
│   ├── rabbit.ts             → RabbitMQ producer/consumer
│   └── backendClient.ts      → Payment API client
│
├── knowledge/
│   ├── payments.md           → Payment lifecycle docs
│   └── fraud-and-access.md   → Fraud rules + RBAC docs
│
└── package.json
```

### 5.4 Frontend (Next.js 15 App Router)

```
frontend/src/
├── app/
│   ├── page.tsx                    → Dashboard/Landing
│   ├── accounts/
│   │   ├── page.tsx                → Accounts list + profile
│   │   └── insights/
│   │       └── [accountNumber]/
│   │           └── page.tsx        → Account analytics + charts
│   ├── payments/
│   │   ├── page.tsx                → Payment list
│   │   └── [id]/
│   │       └── page.tsx            → Payment details
│   ├── admin/
│   │   ├── page.tsx                → Admin dashboard + charts
│   │   └── review/
│   │       └── page.tsx            → Onboarding review queue
│   ├── rules/
│   │   └── page.tsx                → Fraud rule management
│   ├── dlq/
│   │   └── page.tsx                → Dead letter queue
│   ├── events/
│   │   └── page.tsx                → System events feed
│   ├── bot/
│   │   └── page.tsx                → Conversational bot interface
│   ├── source/
│   │   └── page.tsx                → Source account selector (payment)
│   └── destination/
│       └── page.tsx                → Destination account selector (payment)
│
├── components/
│   ├── status-badge.tsx            → Payment status indicator
│   ├── transaction-charts.tsx      → Recharts analytics
│   ├── payment-create-form.tsx     → Payment creation wizard
│   └── [other UI components]
│
└── lib/
    ├── api.ts                      → Backend API client
    ├── onboarding-api.ts           → Onboarding API client
    ├── types.ts                    → TypeScript interfaces
    ├── format.ts                   → Formatting utilities
    └── user-store.ts               → Zustand state management
```

---

## 6. Database Architecture

### 6.1 Database Schema Overview

The RupeeX platform uses **MySQL 8.0** with **InnoDB storage engine** for ACID compliance and referential integrity.

```
┌──────────────────────────────────────────────────────────────┐
│              DATABASE: rupeex_db (MySQL 8.0)                 │
├──────────────────────────────────────────────────────────────┤
│ Tables: 16                                                   │
│ Storage Engine: InnoDB                                       │
│ Character Set: utf8mb4                                       │
│ Collation: utf8mb4_unicode_ci                                │
│ Total Size: ~50MB (seeded with sample data)                  │
└──────────────────────────────────────────────────────────────┘
```

### 6.2 Core Tables

#### **accounts**
```sql
Stores customer account information
├── id (PK)
├── account_number (UNIQUE)
├── account_holder
├── account_type (SAVINGS, CURRENT, BUSINESS)
├── currency (INR, USD, EUR, etc.)
├── country_code
├── balance (DECIMAL 19,2)
├── status (ACTIVE, SUSPENDED)
├── email
├── created_at, updated_at
└── Indexes: account_number, email
```

#### **payments**
```sql
Core payment transaction records
├── id (PK)
├── payment_reference (UNIQUE)
├── amount (DECIMAL 19,2)
├── currency
├── source_account
├── destination_account
├── status (CREATED, VALIDATED, QUEUED, PROCESSING, SENT, SETTLED, FAILED)
├── error_code, error_message
├── payer_email
├── idempotency_key (UNIQUE)
├── source_currency, destination_currency
├── converted_amount, exchange_rate
├── scheduled_at (for future payments)
├── origin_country, destination_country
├── created_at, updated_at
└── Indexes: status, created_at, payer_email
```

#### **payment_history**
```sql
State transition audit trail
├── id (PK)
├── payment_id (FK → payments)
├── old_status
├── new_status
├── reason
├── changed_at
└── Index: payment_id
```

#### **audit_logs**
```sql
Service-level action tracking
├── id (PK)
├── payment_id (FK → payments)
├── service (ValidationEngine, FraudEngine, etc.)
├── action
├── before_state, after_state
├── processing_time_ms
├── reason
├── created_at
└── Index: payment_id
```

### 6.3 Fraud & Risk Tables

#### **fraud_rules**
```sql
Dynamic fraud detection rules
├── id (PK)
├── name (UNIQUE)
├── description
├── rule_type (AMOUNT_THRESHOLD, VELOCITY_CHECK, etc.)
├── threshold (DOUBLE)
├── score_contribution (INT)
├── enabled (BOOLEAN)
├── created_at, updated_at
```

#### **fraud_results**
```sql
Fraud evaluation results per payment
├── id (PK)
├── payment_id (FK → payments)
├── rule_id (FK → fraud_rules)
├── rule_name
├── triggered (BOOLEAN)
├── score_contribution
├── reason
├── created_at
└── Indexes: payment_id, rule_id
```

#### **risk_scores**
```sql
Risk assessment per payment
├── id (PK)
├── payment_id (FK → payments, UNIQUE)
├── score (INT 0-100)
├── category (LOW, MEDIUM, HIGH)
├── explanation (TEXT)
├── decision (APPROVE, REVIEW, REJECT)
├── created_at
```

### 6.4 Queue & Processing Tables

#### **processing_queue**
```sql
Async payment processing queue
├── id (PK)
├── payment_id (FK → payments, UNIQUE)
├── status (READY, PROCESSING, COMPLETED)
├── retry_count
├── max_retries
├── next_retry_at
├── created_at, updated_at
└── Index: status
```

#### **dead_letter_queue**
```sql
Failed payment recovery queue
├── id (PK)
├── payment_id (FK → payments)
├── error_code, error_message
├── retry_count
├── can_retry (BOOLEAN)
├── created_at
└── Index: payment_id, can_retry
```

### 6.5 Notification & Event Tables

#### **notifications**
```sql
Email notification tracking
├── id (PK)
├── recipient_email
├── subject, body
├── status (SENT, FAILED)
├── error_message
├── created_at
└── Index: recipient_email, status
```

#### **system_events**
```sql
Real-time event feed
├── id (PK)
├── event_type (PAYMENT_CREATED, PAYMENT_COMPLETED, etc.)
├── event_details (JSON)
├── created_at
└── Index: event_type, created_at
```

### 6.6 Onboarding Tables (Onboarding Microservice)

#### **customers**
```sql
Customer onboarding profiles
├── id (PK, UUID)
├── customer_id (UNIQUE)
├── full_name
├── email (UNIQUE)
├── phone
├── date_of_birth
├── status (DRAFT, PENDING_REVIEW, APPROVED, REJECTED)
├── account_number (FK → accounts)
├── created_at, updated_at
└── Indexes: customer_id, email, status
```

#### **customer_addresses**
```sql
Customer address information
├── id (PK)
├── customer_id (FK → customers)
├── address_type (HOME, WORK)
├── street, city, state, postal_code, country
├── is_primary (BOOLEAN)
└── Index: customer_id
```

#### **consents**
```sql
Consent management
├── id (PK)
├── customer_id (FK → customers)
├── consent_type (TERMS_AND_CONDITIONS, PRIVACY_POLICY)
├── version
├── accepted (BOOLEAN)
├── accepted_at
└── Index: customer_id, consent_type
```

### 6.7 Entity Relationship Diagram

```mermaid
erDiagram
    ACCOUNTS ||--o{ PAYMENTS : "source/destination"
    PAYMENTS ||--o{ PAYMENT_HISTORY : has
    PAYMENTS ||--o{ AUDIT_LOGS : generates
    PAYMENTS ||--o{ FRAUD_RESULTS : "evaluated_by"
    FRAUD_RULES ||--o{ FRAUD_RESULTS : triggers
    PAYMENTS ||--|| RISK_SCORES : scored_as
    PAYMENTS ||--|| PROCESSING_QUEUE : queued_in
    PAYMENTS ||--o| DEAD_LETTER_QUEUE : may_move_to
    PAYMENTS ||--o{ NOTIFICATIONS : triggers
    PAYMENTS ||--o{ SYSTEM_EVENTS : emits
    CUSTOMERS ||--o| ACCOUNTS : linked_to
    CUSTOMERS ||--o{ CUSTOMER_ADDRESSES : has
    CUSTOMERS ||--o{ CONSENTS : provides
    
    ACCOUNTS {
        BIGINT id PK
        VARCHAR account_number UK
        VARCHAR account_holder
        VARCHAR account_type
        VARCHAR currency
        DECIMAL balance
        VARCHAR status
    }
    
    PAYMENTS {
        BIGINT id PK
        VARCHAR payment_reference UK
        DECIMAL amount
        VARCHAR currency
        VARCHAR source_account FK
        VARCHAR destination_account FK
        VARCHAR status
        VARCHAR idempotency_key UK
        DATETIME scheduled_at
    }
    
    FRAUD_RULES {
        BIGINT id PK
        VARCHAR name UK
        VARCHAR rule_type
        DOUBLE threshold
        INT score_contribution
        BOOLEAN enabled
    }
    
    CUSTOMERS {
        UUID id PK
        VARCHAR customer_id UK
        VARCHAR email UK
        VARCHAR status
        VARCHAR account_number FK
    }
```

---

## 7. Payment Lifecycle

### 7.1 State Machine

```mermaid
stateDiagram-v2
    [*] --> CREATED: Payment Initiated
    CREATED --> VALIDATED: Validation Engine
    VALIDATED --> RISK_ANALYZED: Risk Scoring Engine
    RISK_ANALYZED --> FRAUD_CHECKED: Fraud Detection Engine
    FRAUD_CHECKED --> QUEUED: Queue Manager
    QUEUED --> PROCESSING: Scheduler Picks
    PROCESSING --> SENT: Settlement Engine
    SENT --> SETTLED: Final Settlement
    SETTLED --> [*]
    
    CREATED --> FAILED: Validation Error
    RISK_ANALYZED --> FAILED: High Risk Reject
    FRAUD_CHECKED --> FAILED: Fraud Detected
    PROCESSING --> FAILED: Settlement Error
    
    FAILED --> QUEUED: Retry (if retries < max)
    FAILED --> DLQ: Max Retries Exceeded
    
    QUEUED --> CANCELLED: Manual Cancellation
    DLQ --> QUEUED: Manual Retry
```

### 7.2 State Descriptions

| State | Description | Actions Performed |
|-------|-------------|-------------------|
| **CREATED** | Initial state after payment creation | • Validate idempotency key<br/>• Create payment record<br/>• Log audit event |
| **VALIDATED** | Basic validation passed | • Check account existence<br/>• Validate amount > 0<br/>• Validate currency codes |
| **RISK_ANALYZED** | Risk score calculated | • Calculate risk score (0-100)<br/>• Assign risk category (LOW/MEDIUM/HIGH)<br/>• Make risk decision (APPROVE/REVIEW/REJECT) |
| **FRAUD_CHECKED** | Fraud rules evaluated | • Evaluate all active fraud rules<br/>• Calculate total fraud score<br/>• Store evaluation results |
| **QUEUED** | Added to processing queue | • Create queue entry<br/>• Set retry policy<br/>• Schedule next processing |
| **PROCESSING** | Currently being processed | • Update queue status<br/>• Call settlement engine<br/>• Simulate external gateway call |
| **SENT** | Sent to external gateway | • Mark as sent<br/>• Prepare for settlement |
| **SETTLED** | Final settlement complete | • Mark payment complete<br/>• Send notification<br/>• Publish success event |
| **FAILED** | Processing failed | • Log error details<br/>• Increment retry count<br/>• Move to DLQ if max retries exceeded |
| **CANCELLED** | Manually cancelled | • Remove from queue<br/>• Log cancellation reason |
| **DLQ** | In dead letter queue | • Store failure details<br/>• Allow manual retry |

### 7.3 Processing Flow Sequence

```mermaid
sequenceDiagram
    participant C as Client
    participant API as Payment API
    participant ORCH as Orchestrator
    participant VE as Validation Engine
    participant RE as Risk Engine
    participant FE as Fraud Engine
    participant Q as Queue Manager
    participant SCHED as Scheduler
    participant SE as Settlement Engine
    participant DB as Database
    participant WS as WebSocket
    participant NOTIF as Notification
    
    C->>API: POST /api/payments
    API->>ORCH: createPayment(request)
    ORCH->>DB: Insert payment (CREATED)
    ORCH->>DB: Insert payment_history
    ORCH->>DB: Insert audit_log
    ORCH->>WS: Publish PAYMENT_CREATED
    
    ORCH->>VE: validate(payment)
    VE-->>ORCH: ValidationResult
    ORCH->>DB: Update status (VALIDATED)
    
    ORCH->>RE: scoreRisk(payment)
    RE->>DB: Insert risk_scores
    RE-->>ORCH: RiskScore
    ORCH->>DB: Update status (RISK_ANALYZED)
    
    ORCH->>FE: evaluateFraud(payment)
    FE->>DB: Insert fraud_results
    FE-->>ORCH: FraudResult
    ORCH->>DB: Update status (FRAUD_CHECKED)
    
    ORCH->>Q: enqueue(payment)
    Q->>DB: Insert processing_queue
    ORCH->>DB: Update status (QUEUED)
    ORCH-->>API: PaymentResponse
    API-->>C: 200 OK + Payment Details
    
    loop Async Processing
        SCHED->>DB: Fetch READY queue entries
        SCHED->>DB: Update queue status (PROCESSING)
        SCHED->>DB: Update payment status (PROCESSING)
        SCHED->>SE: settle(payment)
        SE-->>SCHED: SettlementResult
        
        alt Settlement Success
            SCHED->>DB: Update payment status (SENT → SETTLED)
            SCHED->>DB: Delete from processing_queue
            SCHED->>NOTIF: Send success email
            SCHED->>WS: Publish PAYMENT_COMPLETED
        else Settlement Failure (Retries Left)
            SCHED->>DB: Increment retry_count
            SCHED->>DB: Update queue status (READY)
            SCHED->>DB: Set next_retry_at
            SCHED->>WS: Publish PAYMENT_RETRY
        else Settlement Failure (Max Retries)
            SCHED->>DB: Update payment status (FAILED)
            SCHED->>DB: Insert dead_letter_queue
            SCHED->>DB: Delete from processing_queue
            SCHED->>NOTIF: Send failure email
            SCHED->>WS: Publish PAYMENT_FAILED
        end
    end
```

---

## 8. API Architecture

### 8.1 API Endpoint Summary

```
┌────────────────────────────────────────────────────────────┐
│                    PAYMENT API ENDPOINTS                    │
├────────────────────────────────────────────────────────────┤
│ POST   /api/payments                  Create payment       │
│ GET    /api/payments                  List payments        │
│ GET    /api/payments/{id}             Get payment details  │
│ POST   /api/payments/{id}/retry       Retry failed payment │
│ POST   /api/payments/{id}/cancel      Cancel payment       │
│ GET    /api/payments/{id}/history     Get lifecycle history│
├────────────────────────────────────────────────────────────┤
│                   ACCOUNT API ENDPOINTS                     │
├────────────────────────────────────────────────────────────┤
│ GET    /api/accounts                  List all accounts    │
│ POST   /api/accounts                  Create account       │
│ GET    /api/accounts/{id}             Get account details  │
├────────────────────────────────────────────────────────────┤
│                 FRAUD RULE API ENDPOINTS                    │
├────────────────────────────────────────────────────────────┤
│ GET    /api/fraud/rules               List fraud rules     │
│ POST   /api/fraud/rules               Create fraud rule    │
│ PUT    /api/fraud/rules/{id}          Update fraud rule    │
│ DELETE /api/fraud/rules/{id}          Delete fraud rule    │
├────────────────────────────────────────────────────────────┤
│                     DLQ API ENDPOINTS                       │
├────────────────────────────────────────────────────────────┤
│ GET    /api/dlq                       List DLQ entries     │
│ POST   /api/dlq/{id}/retry            Retry from DLQ       │
├────────────────────────────────────────────────────────────┤
│                  METRICS API ENDPOINTS                      │
├────────────────────────────────────────────────────────────┤
│ GET    /api/metrics                   Platform metrics     │
│ GET    /api/dashboard                 Dashboard summary    │
├────────────────────────────────────────────────────────────┤
│                   EVENT API ENDPOINTS                       │
├────────────────────────────────────────────────────────────┤
│ GET    /api/events                    System events feed   │
│ WS     /ws/events (STOMP)             Real-time events     │
├────────────────────────────────────────────────────────────┤
│                EXCHANGE RATE API ENDPOINTS                  │
├────────────────────────────────────────────────────────────┤
│ POST   /api/exchange/convert          Convert currency     │
├────────────────────────────────────────────────────────────┤
│                     OTP API ENDPOINTS                       │
├────────────────────────────────────────────────────────────┤
│ POST   /api/otp/send                  Send OTP email       │
│ POST   /api/otp/verify                Verify OTP           │
├────────────────────────────────────────────────────────────┤
│              NOTIFICATION API ENDPOINTS                     │
├────────────────────────────────────────────────────────────┤
│ POST   /api/notifications/test        Test email sending   │
├────────────────────────────────────────────────────────────┤
│             ONBOARDING API ENDPOINTS                        │
│                  (Onboarding Service)                       │
├────────────────────────────────────────────────────────────┤
│ POST   /onboarding/customers          Create customer      │
│ GET    /onboarding/customers          List customers       │
│ GET    /onboarding/customers/{id}     Get customer         │
│ PUT    /onboarding/customers/{id}     Update customer      │
│ POST   /onboarding/customers/{id}/approve  Approve customer│
│ POST   /onboarding/customers/{id}/reject   Reject customer │
├────────────────────────────────────────────────────────────┤
│                     BOT API ENDPOINTS                       │
│                    (Bot Service)                            │
├────────────────────────────────────────────────────────────┤
│ POST   /api/bot/nl                    Parse NL command     │
│ POST   /api/bot/confirm               Confirm pending cmd  │
│ POST   /api/bot/execute               Execute command      │
│ GET    /slm/status                    SLM model status     │
│ GET    /rag/status                    RAG index status     │
└────────────────────────────────────────────────────────────┘
```

### 8.2 Swagger/OpenAPI Documentation

```
┌──────────────────────────────────────────────────────┐
│         API DOCUMENTATION INTERFACES                 │
├──────────────────────────────────────────────────────┤
│ Swagger UI:     http://localhost:8080/swagger-ui    │
│ OpenAPI JSON:   http://localhost:8080/v3/api-docs   │
│ Health Check:   http://localhost:8080/actuator/health│
└──────────────────────────────────────────────────────┘
```

Features:
- ✅ Interactive API testing
- ✅ Request/response schema documentation
- ✅ Authentication examples
- ✅ Error response documentation
- ✅ Example payloads

---

## 9. Frontend Architecture

### 9.1 Page Structure

```
┌─────────────────────────────────────────────────────────┐
│              NEXT.JS 15 APP ROUTER PAGES                │
├─────────────────────────────────────────────────────────┤
│ / (Dashboard)                                           │
│   → Landing page with platform overview                 │
│                                                         │
│ /accounts                                               │
│   → Account list, profile, payment stats               │
│   → Send payment form with OTP verification             │
│   → Transaction history (sent/received)                 │
│                                                         │
│ /accounts/insights/[accountNumber]                      │
│   → Transaction analytics with charts                   │
│   → Amount trend (line/area chart)                      │
│   → Status distribution (pie chart)                     │
│   → Full transaction table                              │
│                                                         │
│ /payments                                               │
│   → Payment list with filters                           │
│   → Create payment wizard                               │
│   │   • Step 1: Source account selection                │
│   │   • Step 2: Destination account selection           │
│   │   • Step 3: Amount + currency + country             │
│   │   • Step 4: OTP verification                        │
│   │   • Step 5: Confirmation + submission               │
│                                                         │
│ /payments/[id]                                          │
│   → Payment details page                                │
│   → Status timeline visualization                       │
│   → Fraud & risk analysis results                       │
│   → Audit trail                                         │
│   → Actions: Cancel, Retry                              │
│                                                         │
│ /admin                                                  │
│   → Platform-wide metrics dashboard                     │
│   → Count metrics (line chart)                          │
│   → Value metrics (bar chart)                           │
│   → Status breakdown                                    │
│   → Account activity table                              │
│   → Recent payments feed                                │
│                                                         │
│ /admin/review                                           │
│   → Onboarding customer review queue                    │
│   → Approve/reject workflow                             │
│   → Customer details + consent status                   │
│                                                         │
│ /rules                                                  │
│   → Fraud rule management                               │
│   → Create/edit/delete fraud rules                      │
│   → Enable/disable rules                                │
│   → Rule effectiveness metrics                          │
│                                                         │
│ /dlq                                                    │
│   → Dead letter queue interface                         │
│   → Failed payment list                                 │
│   → Retry controls                                      │
│   → Failure analytics                                   │
│                                                         │
│ /events                                                 │
│   → Real-time system event feed                         │
│   → WebSocket-powered live updates                      │
│   → Event filtering                                     │
│                                                         │
│ /bot                                                    │
│   → Conversational chat interface                       │
│   → Natural language payment commands                   │
│   → Confirmation workflow for high-value txns           │
│   → Command history                                     │
│                                                         │
│ /source, /destination                                   │
│   → Account selection pages (payment wizard steps)      │
└─────────────────────────────────────────────────────────┘
```

### 9.2 State Management

```typescript
┌────────────────────────────────────────────────┐
│            STATE MANAGEMENT LAYERS             │
├────────────────────────────────────────────────┤
│ Zustand Stores:                                │
│   • userStore     → Current user + role        │
│   • paymentsStore → Payment creation state     │
│                                                │
│ React Query:                                   │
│   • usePayments()       → Payment list         │
│   • usePaymentDetails() → Payment by ID        │
│   • useAccounts()       → Account list         │
│   • useFraudRules()     → Fraud rules          │
│   • useDLQEntries()     → DLQ entries          │
│   • useSystemEvents()   → Event feed           │
│                                                │
│ WebSocket:                                     │
│   • STOMP client connection                    │
│   • Subscribe: /topic/events                   │
│   • Auto-reconnect on disconnect               │
│   • Event-driven UI updates                    │
└────────────────────────────────────────────────┘
```

### 9.3 Component Architecture

```
┌────────────────────────────────────────────────┐
│         REUSABLE COMPONENT LIBRARY             │
├────────────────────────────────────────────────┤
│ Core Components:                               │
│   • StatusBadge         → Payment status chip  │
│   • TransactionCharts   → Recharts wrapper     │
│   • PaymentCreateForm   → Multi-step wizard    │
│   • AccountSelector     → Account picker       │
│   • UserSwitcher        → Role/user switcher   │
│                                                │
│ Radix UI Primitives:                           │
│   • Dialog, DropdownMenu, Slot               │
│                                                │
│ Utility Components:                            │
│   • Loading spinners                           │
│   • Error boundaries                           │
│   • Empty states                               │
│   • Toast notifications                        │
└────────────────────────────────────────────────┘
```

### 9.4 Charting & Visualization

```
┌────────────────────────────────────────────────┐
│          CHARTS & VISUALIZATIONS               │
├────────────────────────────────────────────────┤
│ Recharts Library:                              │
│   • LineChart    → Trend analysis              │
│   • BarChart     → Volume comparison           │
│   • AreaChart    → Cumulative trends           │
│   • PieChart     → Status distribution         │
│                                                │
│ Features:                                      │
│   • Responsive design                          │
│   • Interactive tooltips                       │
│   • Custom color schemes                       │
│   • Real-time data updates                     │
│   • Export capabilities                        │
│                                                │
│ Time Granularity Filters:                      │
│   • Per Hour, Per Day, Per Month, Per Year     │
└────────────────────────────────────────────────┘
```

---

## 10. Security & Compliance

### 10.1 Security Features

```
┌────────────────────────────────────────────────────────┐
│              SECURITY IMPLEMENTATION                   │
├────────────────────────────────────────────────────────┤
│ Authentication:                                        │
│   ✅ OTP-based email authentication                    │
│   ✅ 4-digit numeric OTP with expiry                   │
│   ✅ Rate limiting on OTP requests                     │
│   ✅ OTP hashing/encryption in transit                 │
│                                                        │
│ Authorization:                                         │
│   ✅ Role-Based Access Control (RBAC)                  │
│   ✅ Admin vs Member role segregation                  │
│   ✅ Account-level data isolation                      │
│   ✅ API endpoint permission checks                    │
│                                                        │
│ Data Protection:                                       │
│   ✅ HTTPS encryption (via NGINX)                      │
│   ✅ Sensitive data masking in logs                    │
│   ✅ Email masking in UI (e.g., us***@example.com)    │
│   ✅ Database connection encryption                    │
│                                                        │
│ Payment Security:                                      │
│   ✅ Idempotency key validation                        │
│   ✅ Duplicate payment prevention                      │
│   ✅ Transaction amount limits                         │
│   ✅ Cross-border fraud checks                         │
│                                                        │
│ Fraud Prevention:                                      │
│   ✅ Real-time fraud rule evaluation                   │
│   ✅ Velocity checks (transaction frequency)            │
│   ✅ Blacklist screening                               │
│   ✅ Risk-based decision engine                        │
│                                                        │
│ Bot Security:                                          │
│   ✅ High-value transaction confirmation               │
│   ✅ Command validation                                │
│   ✅ Rate limiting on bot endpoints                    │
│   ✅ Intent parsing safeguards                         │
└────────────────────────────────────────────────────────┘
```

### 10.2 Compliance Features

```
┌────────────────────────────────────────────────────────┐
│            COMPLIANCE & AUDIT FEATURES                 │
├────────────────────────────────────────────────────────┤
│ Audit Trail:                                           │
│   ✅ Complete payment lifecycle history                 │
│   ✅ State transition timestamps                        │
│   ✅ Actor/user tracking                                │
│   ✅ Before/after state capture                         │
│   ✅ Processing time metrics                            │
│   ✅ Immutable audit logs                               │
│                                                        │
│ Consent Management:                                    │
│   ✅ Terms & conditions consent capture                │
│   ✅ Privacy policy consent tracking                   │
│   ✅ Consent versioning                                │
│   ✅ Consent withdrawal support                        │
│   ✅ Consent audit trail                               │
│                                                        │
│ Data Retention:                                        │
│   ✅ Configurable retention policies                   │
│   ✅ Event log archival                                │
│   ✅ Historical data access                            │
│                                                        │
│ Reporting:                                             │
│   ✅ Transaction reporting                             │
│   ✅ Fraud detection reports                           │
│   ✅ Risk analysis reports                             │
│   ✅ Compliance audit exports                          │
│                                                        │
│ Standards Alignment:                                   │
│   ✅ ISO 4217 currency codes                           │
│   ✅ ISO 8601 timestamp format                         │
│   ✅ RESTful API best practices                        │
│   ✅ OpenAPI 3.0 specification                         │
└────────────────────────────────────────────────────────┘
```

---

## 11. Deployment Architecture

### 11.1 Docker Compose Stack

```
┌──────────────────────────────────────────────────────────┐
│           DOCKER COMPOSE MULTI-CONTAINER STACK           │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  ┌────────────┐    ┌────────────┐    ┌────────────┐    │
│  │   NGINX    │───►│  Frontend  │    │   Ollama   │    │
│  │  Port 80   │    │ Next.js 15 │    │   (LLM)    │    │
│  └─────┬──────┘    └────────────┘    └──────┬─────┘    │
│        │                                     │          │
│        │           ┌────────────┐            │          │
│        ├──────────►│   Backend  │◄───────────┘          │
│        │           │Spring Boot │                       │
│        │           └──────┬─────┘                       │
│        │                  │                             │
│        │           ┌──────▼─────┐                       │
│        │           │   MySQL    │                       │
│        │           │  Database  │                       │
│        │           └────────────┘                       │
│        │                                                │
│        │           ┌────────────┐    ┌────────────┐    │
│        ├──────────►│ Onboarding │    │  RabbitMQ  │    │
│        │           │  Service   │    │  (Queue)   │    │
│        │           └────────────┘    └──────┬─────┘    │
│        │                                    │          │
│        │           ┌────────────┐    ┌──────▼─────┐    │
│        └──────────►│Bot Service │◄───┤Bot Worker  │    │
│                    │  (HTTP)    │    │ (Consumer) │    │
│                    └────────────┘    └────────────┘    │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

### 11.2 Service Configuration

| Service | Image/Build | Ports | Dependencies | Health Check |
|---------|-------------|-------|--------------|--------------|
| **MySQL** | mysql:8.0 | 3306 | - | mysqladmin ping |
| **Backend** | Java 21 + Maven | 8080 | db | - |
| **Onboarding** | Java 21 + Maven | 8083 | db | - |
| **Frontend** | Node 20 + Next.js | 3000 | app, onboarding-app | - |
| **NGINX** | nginx:1.27-alpine | 80 | app, frontend, onboarding-app | - |
| **RabbitMQ** | rabbitmq:3.11-management | 5672, 15672 | - | - |
| **Ollama** | ollama/ollama:latest | 11434 | - | ollama list |
| **Bot Service** | Node 20 + TypeScript | 4001 | rabbitmq, ollama, app | - |
| **Bot Worker** | Node 20 + TypeScript | - | rabbitmq, app | - |

### 11.3 Volume Management

```
Persistent Volumes:
• db_data        → MySQL database files
• ollama_data    → LLM models (Qwen 2.5, Nomic Embed)
```

### 11.4 Network Architecture

```
┌────────────────────────────────────────────────────┐
│               NETWORK TOPOLOGY                     │
├────────────────────────────────────────────────────┤
│ External → NGINX (Port 80)                         │
│              │                                     │
│              ├→ /          → Frontend (3000)       │
│              ├→ /api/*     → Backend (8080)        │
│              ├→ /onboarding/* → Onboarding (8083) │
│              └→ /api/bot/* → Bot Service (4001)   │
│                                                    │
│ Internal Docker Network:                           │
│   • app.default                                    │
│   • onboarding-app.default                         │
│   • frontend.default                               │
│   • db.default                                     │
│   • rabbitmq.default                               │
│   • ollama.default                                 │
│   • bot-service.default                            │
│   • bot-worker.default                             │
└────────────────────────────────────────────────────┘
```

### 11.5 Environment Configuration

```bash
# Database
MYSQL_ROOT_PASSWORD=root
MYSQL_DATABASE=rupeex_db
MYSQL_USER=rupeex
MYSQL_PASSWORD=rupeex

# Backend
SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/rupeex_db
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080

# Frontend
API_BASE_URL=http://app:8080/api  (server-side)
NEXT_PUBLIC_API_BASE_URL=          (client-side, proxied via nginx)
ONBOARDING_BASE_URL=http://onboarding-app:8083/onboarding

# Bot Service
AMQP_URL=amqp://guest:guest@rabbitmq:5672
PAYMENT_API_URL=http://app:8080/api
SLM_ENABLED=true
SLM_BASE_URL=http://ollama:11434
SLM_MODEL=qwen2.5:0.5b
EMBED_MODEL=nomic-embed-text
RAG_ENABLED=true

# OTP
OTP_FALLBACK_ENABLED=true

# Exchange Rate API
EXCHANGE_API_KEY=demo-key or your_api_key
```

---

## 12. Integration Architecture

### 12.1 External Integrations

```
┌────────────────────────────────────────────────────────┐
│            EXTERNAL API INTEGRATIONS                   │
├────────────────────────────────────────────────────────┤
│ ExchangeRate-API (v6)                                  │
│   • Real-time currency conversion                      │
│   • 160+ currency pairs supported                      │
│   • Rate caching strategy                              │
│   • Fallback on API failure                            │
│   → GET https://v6.exchangerate-api.com/v6/{key}/latest/{currency}
│                                                        │
│ SMTP Email Service                                     │
│   • OTP delivery                                       │
│   • Transaction notifications                          │
│   • Failure alerts                                     │
│   • Configurable SMTP host                             │
│   → spring.mail.host, spring.mail.port                 │
└────────────────────────────────────────────────────────┘
```

### 12.2 Internal Integrations

```
┌────────────────────────────────────────────────────────┐
│         MICROSERVICE COMMUNICATION PATTERNS            │
├────────────────────────────────────────────────────────┤
│ Payment ←→ Onboarding                                  │
│   • REST API calls                                     │
│   • GET /onboarding/customers (list approved users)    │
│   • Customer status validation                         │
│                                                        │
│ Frontend ←→ Backend                                    │
│   • REST API over HTTPS                                │
│   • WebSocket (STOMP) for real-time events             │
│   • Server-side rendering with API proxy               │
│                                                        │
│ Bot Service ←→ Payment API                             │
│   • REST API calls                                     │
│   • POST /api/payments (create payment)                │
│   • GET /api/payments (list payments)                  │
│                                                        │
│ Bot Service ←→ RabbitMQ                                │
│   • Command queue producer                             │
│   • Worker queue consumer                              │
│   • Async command execution                            │
│                                                        │
│ Bot Service ←→ Ollama                                  │
│   • HTTP REST API                                      │
│   • POST /api/generate (LLM inference)                 │
│   • POST /api/embeddings (text embedding)              │
└────────────────────────────────────────────────────────┘
```

---

## 13. Operational Features

### 13.1 Monitoring & Observability

```
┌────────────────────────────────────────────────────────┐
│          OBSERVABILITY IMPLEMENTATION                  │
├────────────────────────────────────────────────────────┤
│ Real-Time Metrics:                                     │
│   • Total payments processed                           │
│   • Success/failure rates                              │
│   • Average processing time                            │
│   • Queue depth                                        │
│   • DLQ entries count                                  │
│   • Active fraud rules                                 │
│                                                        │
│ Event Streaming:                                       │
│   • WebSocket event feed                               │
│   • Event types tracked:                               │
│     - PAYMENT_CREATED                                  │
│     - PAYMENT_VALIDATED                                │
│     - PAYMENT_QUEUED                                   │
│     - PAYMENT_PROCESSING                               │
│     - PAYMENT_COMPLETED                                │
│     - PAYMENT_FAILED                                   │
│     - PAYMENT_RETRY                                    │
│   • Real-time UI updates                               │
│                                                        │
│ Health Checks:                                         │
│   • Spring Actuator endpoints                          │
│   • Database connectivity                              │
│   • External API status                                │
│   • Queue health                                       │
│                                                        │
│ Logging:                                               │
│   • Structured JSON logging                            │
│   • Correlation ID tracking                            │
│   • Log levels: DEBUG, INFO, WARN, ERROR               │
│   • Log aggregation ready                              │
└────────────────────────────────────────────────────────┘
```

### 13.2 Operational Dashboards

```
┌────────────────────────────────────────────────────────┐
│               ADMIN DASHBOARD FEATURES                 │
├────────────────────────────────────────────────────────┤
│ Platform Metrics Cards:                                │
│   • Total Payments                 (Count)             │
│   • Total Volume                   (Currency sum)      │
│   • Success Rate                   (Percentage)        │
│   • Failure Rate                   (Percentage)        │
│   • In-flight                      (Count)             │
│   • Settled Value                  (Currency sum)      │
│   • Total Accounts                 (Count)             │
│   • Avg Payment                    (Currency avg)      │
│                                                        │
│ Charts & Visualizations:                               │
│   • Count Metrics (Line Chart)                         │
│     - Total Payments, Settled, Failed, In-flight       │
│   • Value Metrics (Bar Chart)                          │
│     - Total Volume, Settled Value, Avg Payment         │
│   • Status Breakdown (Mini Bar Chart)                  │
│     - Payment state distribution                       │
│                                                        │
│ Operational Tables:                                    │
│   • Account Activity                                   │
│     - Sent/received counts and amounts per account     │
│   • Recent Payments                                    │
│     - Latest 8 payments across platform                │
│                                                        │
│ Quick Links:                                           │
│   • Fraud Rules, DLQ, System Events, Accounts          │
└────────────────────────────────────────────────────────┘
```

### 13.3 CI/CD Pipeline

```
┌────────────────────────────────────────────────────────┐
│              JENKINS CI/CD PIPELINE                    │
├────────────────────────────────────────────────────────┤
│ Stages:                                                │
│                                                        │
│ 1. Checkout                                            │
│    → Pull latest code from repository                  │
│                                                        │
│ 2. Generate .env                                       │
│    → Inject secrets from Jenkins credentials           │
│                                                        │
│ 3. Docker Sanity Check                                 │
│    → Verify Docker daemon availability                 │
│                                                        │
│ 4. Stop Previous Deployment                            │
│    → docker-compose down --remove-orphans              │
│                                                        │
│ 5. Build Images                                        │
│    → docker-compose build                              │
│    → Multi-stage builds for backend (Maven + JRE)      │
│    → Node.js builds for frontend and bot               │
│                                                        │
│ 6. Deploy Stack                                        │
│    → docker-compose up -d                              │
│    → Wait for health checks                            │
│                                                        │
│ 7. Smoke Test                                          │
│    → Verify containers are running                     │
│    → Check application logs                            │
│                                                        │
│ 8. Cleanup                                             │
│    → Remove workspace files (optional)                 │
│                                                        │
│ Notifications:                                         │
│    → Success/failure email alerts                      │
│    → Slack/Teams integration (optional)                │
└────────────────────────────────────────────────────────┘
```

---

## 14. Flow Diagrams

### 14.1 Complete System Data Flow

```mermaid
graph TB
    subgraph "Client Layer"
        USER[User Browser]
    end
    
    subgraph "Gateway"
        NGINX[NGINX Reverse Proxy]
    end
    
    subgraph "Frontend"
        NEXT[Next.js Frontend]
        WS_CLIENT[WebSocket Client]
    end
    
    subgraph "Backend Services"
        API[Payment API]
        ONBOARD_API[Onboarding API]
        BOT_API[Bot Service API]
    end
    
    subgraph "Business Logic"
        ORCH[Payment Orchestrator]
        FRAUD[Fraud Engine]
        RISK[Risk Engine]
        QUEUE[Queue Scheduler]
        SETTLE[Settlement Engine]
    end
    
    subgraph "Data Layer"
        DB[(MySQL Database)]
        MQ[RabbitMQ]
    end
    
    subgraph "AI/ML"
        OLLAMA[Ollama LLM/RAG]
    end
    
    subgraph "External"
        EXCHANGE_API[ExchangeRate API]
        SMTP[SMTP Server]
    end
    
    USER --> NGINX
    NGINX --> NEXT
    NGINX --> API
    NGINX --> ONBOARD_API
    NGINX --> BOT_API
    
    NEXT --> API
    NEXT --> ONBOARD_API
    NEXT --> BOT_API
    WS_CLIENT -.->|Subscribe /topic/events| API
    
    API --> ORCH
    ORCH --> FRAUD
    ORCH --> RISK
    ORCH --> QUEUE
    QUEUE --> SETTLE
    
    API --> DB
    ONBOARD_API --> DB
    FRAUD --> DB
    RISK --> DB
    QUEUE --> DB
    
    BOT_API --> MQ
    BOT_API --> OLLAMA
    BOT_API --> API
    
    SETTLE --> EXCHANGE_API
    SETTLE --> SMTP
    API -.->|Publish events| WS_CLIENT
```

### 14.2 Payment Creation Flow

```mermaid
flowchart TD
    START([User Initiates Payment]) --> SELECT_SRC[Select Source Account]
    SELECT_SRC --> SELECT_DEST[Select Destination Account]
    SELECT_DEST --> ENTER_AMOUNT[Enter Amount, Currency, Country]
    ENTER_AMOUNT --> SEND_OTP[Request OTP]
    SEND_OTP --> OTP_EMAIL[System Sends OTP Email]
    OTP_EMAIL --> ENTER_OTP[User Enters OTP]
    ENTER_OTP --> VERIFY_OTP{OTP Valid?}
    VERIFY_OTP -->|No| OTP_ERROR[Show Error]
    OTP_ERROR --> ENTER_OTP
    VERIFY_OTP -->|Yes| SUBMIT_PAYMENT[Submit Payment]
    SUBMIT_PAYMENT --> API_CREATE[POST /api/payments]
    API_CREATE --> ORCH_START[Orchestrator: createPayment]
    ORCH_START --> DB_INSERT[Insert Payment: CREATED]
    DB_INSERT --> VALIDATE[Validation Engine]
    VALIDATE --> RISK_SCORE[Risk Scoring Engine]
    RISK_SCORE --> FRAUD_CHECK[Fraud Detection Engine]
    FRAUD_CHECK --> ENQUEUE[Add to Processing Queue]
    ENQUEUE --> RESPONSE[Return Payment Response]
    RESPONSE --> UI_UPDATE[UI Shows Payment Created]
    UI_UPDATE --> WS_EVENT[WebSocket: PAYMENT_CREATED]
    
    ENQUEUE --> ASYNC[Async Processing Begins]
    ASYNC --> SCHEDULER[Scheduler Picks Queue Entry]
    SCHEDULER --> PROCESS[Process Payment]
    PROCESS --> SETTLE{Settlement Success?}
    SETTLE -->|Yes| SUCCESS[Payment SETTLED]
    SETTLE -->|No, Retries Left| RETRY[Increment Retry, Re-queue]
    SETTLE -->|No, Max Retries| FAILED[Payment FAILED, Move to DLQ]
    
    SUCCESS --> NOTIF_SUCCESS[Send Success Notification]
    FAILED --> NOTIF_FAIL[Send Failure Notification]
    RETRY --> SCHEDULER
    
    NOTIF_SUCCESS --> END([Payment Complete])
    NOTIF_FAIL --> END
```

### 14.3 Fraud Detection Flow

```mermaid
flowchart LR
    PAYMENT[Payment Created] --> FETCH_RULES[Fetch Active Fraud Rules]
    FETCH_RULES --> RULE1{Amount Threshold<br/>Rule}
    FETCH_RULES --> RULE2{Velocity Check<br/>Rule}
    FETCH_RULES --> RULE3{Cross-Border<br/>Rule}
    FETCH_RULES --> RULE4{Blacklist<br/>Rule}
    
    RULE1 -->|Triggered| SCORE1[+30 points]
    RULE1 -->|Not Triggered| SCORE1[+0 points]
    
    RULE2 -->|Triggered| SCORE2[+25 points]
    RULE2 -->|Not Triggered| SCORE2[+0 points]
    
    RULE3 -->|Triggered| SCORE3[+20 points]
    RULE3 -->|Not Triggered| SCORE3[+0 points]
    
    RULE4 -->|Triggered| SCORE4[+50 points]
    RULE4 -->|Not Triggered| SCORE4[+0 points]
    
    SCORE1 --> SUM[Calculate Total Fraud Score]
    SCORE2 --> SUM
    SCORE3 --> SUM
    SCORE4 --> SUM
    
    SUM --> STORE[Store Fraud Results in DB]
    STORE --> RETURN[Return to Orchestrator]
```

### 14.4 Bot Command Flow

```mermaid
sequenceDiagram
    participant U as User
    participant UI as Bot Interface
    participant API as Bot Service API
    participant SLM as Ollama LLM
    participant RAG as RAG Index
    participant Q as RabbitMQ
    participant W as Bot Worker
    participant PAY as Payment API
    
    U->>UI: Types: "Send 5000 INR to AC123456"
    UI->>API: POST /api/bot/nl {text: "..."}
    API->>RAG: Find relevant context
    RAG-->>API: Top 3 knowledge chunks
    API->>SLM: Generate intent with RAG context
    SLM-->>API: {intent: "CREATE_PAYMENT", amount: 5000, ...}
    
    alt High-value transaction
        API-->>UI: {requiresConfirmation: true, commandId: "xyz"}
        UI->>U: Show confirmation prompt
        U->>UI: Click "Confirm"
        UI->>API: POST /api/bot/confirm {commandId: "xyz"}
    end
    
    API->>Q: Publish command to queue
    API-->>UI: {queued: true}
    
    Q->>W: Consume command message
    W->>PAY: Execute payment creation
    PAY-->>W: Payment created successfully
    W->>UI: Notify completion (via WebSocket)
    UI->>U: Show success message
```

### 14.5 Scheduled Payment Release Flow

```mermaid
flowchart TD
    SCHEDULER[Scheduled Payment Release Scheduler] --> CHECK[Check for Due Payments]
    CHECK --> QUERY[SELECT * FROM payments<br/>WHERE scheduled_at <= NOW()<br/>AND status = 'CREATED']
    QUERY --> HAS_PAYMENTS{Payments Found?}
    HAS_PAYMENTS -->|No| SLEEP[Sleep 1 minute]
    HAS_PAYMENTS -->|Yes| LOOP[For Each Payment]
    
    LOOP --> ENQUEUE[Add to Processing Queue]
    ENQUEUE --> UPDATE[Update Status: QUEUED]
    UPDATE --> LOG[Log Audit Event]
    LOG --> PUBLISH[Publish WebSocket Event]
    PUBLISH --> LOOP
    
    PUBLISH --> ASYNC[Async Queue Processing Begins]
    SLEEP --> CHECK
```

---

## 15. Summary

### Project Statistics

```
┌────────────────────────────────────────────────────────┐
│              PROJECT METRICS & STATISTICS              │
├────────────────────────────────────────────────────────┤
│ Backend Code:                                          │
│   • Java Classes:        ~80                           │
│   • REST Controllers:    11                            │
│   • Service Classes:     15                            │
│   • JPA Entities:        16                            │
│   • Repositories:        15                            │
│   • API Endpoints:       ~50                           │
│                                                        │
│ Frontend Code:                                         │
│   • Next.js Pages:       13                            │
│   • React Components:    ~25                           │
│   • API Client Functions: ~30                          │
│   • State Stores:        2 (Zustand)                   │
│                                                        │
│ Database:                                              │
│   • Tables:              16                            │
│   • Indexes:             ~25                           │
│   • Foreign Keys:        ~15                           │
│                                                        │
│ Docker Services:         9 containers                  │
│ Microservices:           3 (Payment, Onboarding, Bot)  │
│ External APIs:           2 (ExchangeRate, SMTP)        │
│ Message Queues:          2 (bot commands, bot tasks)   │
│                                                        │
│ Lines of Code (approx):                                │
│   • Backend (Java):      ~15,000                       │
│   • Frontend (TS/TSX):   ~8,000                        │
│   • Bot Service (TS):    ~2,000                        │
│   • SQL Schema:          ~500                          │
│   • Configuration:       ~1,000                        │
│                                                        │
│ Documentation:                                         │
│   • Main Docs:           8 MD files                    │
│   • API Docs:            Swagger/OpenAPI               │
│   • README files:        5                             │
│   • Total Doc Lines:     ~3,000                        │
└────────────────────────────────────────────────────────┘
```

### Key Achievements

✅ **Production-Ready Architecture**
- Clean architecture with clear separation of concerns
- Microservices-based design for scalability
- Containerized deployment with Docker Compose

✅ **Complete Payment Lifecycle**
- 8-state workflow with state machine validation
- Automated retry and recovery mechanisms
- Dead letter queue for failed transactions

✅ **Advanced Fraud Detection**
- Dynamic rule engine with 6+ rule types
- Real-time fraud evaluation
- Explainable fraud decisions

✅ **Rich User Experience**
- Modern Next.js 15 frontend with TypeScript
- Real-time updates via WebSocket
- Interactive charts and analytics
- Natural language bot interface

✅ **Enterprise Security**
- OTP-based authentication
- Role-based access control
- Complete audit trail
- Data encryption in transit

✅ **AI/ML Integration**
- On-premise LLM (Ollama + Qwen 2.5)
- RAG-powered knowledge retrieval
- Natural language payment commands

✅ **Operational Excellence**
- Real-time monitoring dashboards
- Comprehensive logging and auditing
- CI/CD pipeline with Jenkins
- Health checks and auto-recovery

---

## 16. Future Enhancements

### Planned Features

```
Phase 1 (Immediate):
□ Enhanced fraud detection algorithms
□ ML-based risk scoring models
□ Advanced reporting and analytics
□ Mobile app (React Native)

Phase 2 (Next Quarter):
□ Multi-region deployment
□ Kafka for event streaming
□ Redis for distributed caching
□ Advanced webhook system

Phase 3 (Long-term):
□ Blockchain integration for settlement
□ Open Banking API connections
□ Advanced KYC/AML workflows
□ AI-powered fraud prediction
□ Multi-language support
```

---

## Appendix

### A. Technology Versions

| Technology | Version |
|-----------|---------|
| Java | 21 (OpenJDK Temurin) |
| Spring Boot | 3.x |
| MySQL | 8.0 |
| Next.js | 15.3.5 |
| React | 19.1.1 |
| TypeScript | 5.x |
| Node.js | 20.x |
| Docker | 24.x |
| NGINX | 1.27 |
| RabbitMQ | 3.11 |
| Ollama | Latest |

### B. Important URLs

```
Local Development:
• Frontend:         http://localhost:3000
• Backend API:      http://localhost:8080/api
• Onboarding API:   http://localhost:8083/onboarding
• Swagger UI:       http://localhost:8080/swagger-ui
• RabbitMQ Admin:   http://localhost:15672
• Ollama API:       http://localhost:11434
• Bot Service:      http://localhost:4001

Docker Compose:
• NGINX Gateway:    http://localhost:80
• All services accessible through NGINX reverse proxy
```

### C. Repository Structure

```
RupeeX-payment-processing/
├── backend/                  (Payment Processing Service)
├── onboarding-service/       (Customer Onboarding Service)
├── bot-service/              (Bot & NL Interface Service)
├── frontend/                 (Next.js Frontend Application)
├── nginx/                    (NGINX Configuration)
├── Documentation/            (Project Documentation)
├── Diagrams/                 (Architecture Diagrams)
├── scripts/                  (Utility Scripts)
├── docker-compose.yml        (Multi-container Orchestration)
├── Jenkinsfile               (CI/CD Pipeline Definition)
├── README.md                 (Quick Start Guide)
└── .env.example              (Environment Configuration Template)
```

---

**Document Version:** 1.0  
**Last Updated:** August 6, 2026  
**Prepared By:** RupeeX Development Team  
**Contact:** admin@rupeex.example.com  

---

*This documentation is intended for technical presentations, stakeholder reviews, and system architecture discussions. For detailed API specifications, refer to Swagger/OpenAPI documentation at `/swagger-ui`.*

