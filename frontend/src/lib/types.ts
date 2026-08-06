/**
 * Domain types mirroring the Spring Boot backend entities
 * (com.rupeex.main.model.*). Keep these in sync with the backend
 * when fields change.
 */

export type PaymentStatus =
  | "CREATED"
  | "SCHEDULED"
  | "VALIDATED"
  | "RISK_ANALYZED"
  | "FRAUD_CHECKED"
  | "QUEUED"
  | "PENDING"
  | "PROCESSING"
  | "SENT"
  | "SETTLED"
  | "SUCCESS"
  | "COMPLETED"
  | "FAILED"
  | "CANCELLED"
  | "REVERSED"
  | "PENDING_ADMIN_APPROVAL";

export type AccountStatus = "ACTIVE" | "INACTIVE" | "SUSPENDED" | "CLOSED";

export interface FraudResultDetail {
  id: number;
  paymentId: number;
  ruleId: number;
  ruleName: string;
  triggered: boolean;
  scoreContribution: number;
  reason: string;
}

export interface RiskScoreDetail {
  id: number;
  paymentId: number;
  score: number;
  category: string;
  explanation: string;
  decision: string; // "Auto Process" or "Manual Review"
  createdAt: string;
}

export interface Payment {
  id: number;
  amount: string; // BigDecimal serialized as string/number by Jackson
  currency: string;
  sourceAccount: string;
  destinationAccount: string;
  reference?: string;
  status: PaymentStatus | string;
  errorCode?: string;
  errorMessage?: string;
  idempotencyKey: string;
  createdAt: string;
  updatedAt: string;
  /** IST timestamp the payment is scheduled to be released, if any. */
  scheduledAt?: string;
  riskScore?: RiskScoreDetail;
  fraudResults?: FraudResultDetail[];
}

export interface CreatePaymentInput {
  amount: number;
  currency: string;
  sourceAccount: string;
  destinationAccount: string;
  originCountry: string;
  destinationCountry: string;
  idempotencyKey?: string;
  payerEmail?: string;
  /** Optional future IST datetime ("YYYY-MM-DDTHH:mm") to schedule the payment. */
  scheduledAt?: string;
}

export interface Account {
  id: number;
  accountNumber: string;
  accountHolder: string;
  accountType: string;
  currency: string;
  countryCode?: string;
  balance?: string | number;
  bankName?: string;
  bankCode?: string;
  ifscCode?: string;
  swiftCode?: string;
  status: AccountStatus | string;
  email?: string;
  metadata?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PaymentStatusHistoryEntry {
  id: number;
  paymentId: number;
  oldStatus?: PaymentStatus | string;
  newStatus: PaymentStatus | string;
  changedAt: string;
  reason?: string;
}

export interface DashboardStats {
  totalVolume: number;
  totalPayments: number;
  successRate: number;
  activeAccounts: number;
}

export interface SystemEvent {
  id: number;
  eventType: string;
  entityId?: number;
  payload: string;
  createdAt: string;
}

export type FraudRuleType =
  | "LARGE_TRANSACTION"
  | "NIGHT_TRANSACTION"
  | "VELOCITY_CHECK"
  | "REPEATED_FAILED_ATTEMPTS"
  | "BLACKLISTED_ACCOUNT"
  | "HIGH_RISK_COUNTRY"
  | "NEW_ACCOUNT"
  | "SUSPICIOUS_FREQUENCY";

export interface FraudRule {
  id: number;
  name: string;
  description: string;
  ruleType: FraudRuleType;
  threshold: number;
  scoreContribution: number;
  enabled: boolean;
}

export type FraudRuleInput = Omit<FraudRule, "id">;

export interface DeadLetterEntry {
  id: number;
  paymentId: number;
  reason: string;
  lastRetryCount: number;
  createdAt?: string;
}

export interface ExchangeRateResult {
  originalAmount: number;
  fromCurrency: string;
  toCurrency: string;
  exchangeRate: number;
  convertedAmount: number;
}
