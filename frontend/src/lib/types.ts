/**
 * Domain types mirroring the Spring Boot backend entities
 * (com.rupeex.main.model.*). Keep these in sync with the backend
 * when fields change.
 */

export type PaymentStatus =
  | "PENDING"
  | "PROCESSING"
  | "COMPLETED"
  | "FAILED"
  | "CANCELLED"
  | "REVERSED";

export type AccountStatus = "ACTIVE" | "INACTIVE" | "SUSPENDED" | "CLOSED";

export interface Payment {
  id: number;
  amount: string; // BigDecimal serialized as string/number by Jackson
  currency: string;
  sourceAccount: string;
  destinationAccount: string;
  reference?: string;
  status: PaymentStatus | string;
  errorCode?: string;
  idempotencyKey: string;
  createdAt: string;
  updatedAt: string;
}

export interface Account {
  id: number;
  accountNumber: string;
  accountHolder: string;
  accountType: string;
  currency: string;
  bankName?: string;
  bankCode?: string;
  ifscCode?: string;
  swiftCode?: string;
  status: AccountStatus | string;
  metadata?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PaymentStatusHistoryEntry {
  id: number;
  paymentId: number;
  status: PaymentStatus | string;
  changedAt: string;
  remarks?: string;
  changedBy?: string;
}

export interface DashboardStats {
  totalVolume: number;
  totalPayments: number;
  successRate: number;
  activeAccounts: number;
}
