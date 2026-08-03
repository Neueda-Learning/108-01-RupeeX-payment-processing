import type {
  Account,
  CreatePaymentInput,
  DeadLetterEntry,
  FraudRule,
  FraudRuleInput,
  DashboardStats,
  Payment,
  PaymentStatusHistoryEntry,
  SystemEvent,
} from "./types";
import { mockAccounts, mockPayments, mockStats } from "./mock-data";

/**
 * Base URL of the RupeeX Spring Boot backend, resolved server-side.
 *
 * `API_BASE_URL` (no NEXT_PUBLIC_ prefix) is read at runtime on the
 * server/container and is the preferred variable — it lets the same
 * built image point at different backends (e.g. the `app` service name
 * inside docker-compose) without rebuilding. `NEXT_PUBLIC_API_BASE_URL`
 * is kept as a fallback for local `npm run dev` convenience.
 */
export const API_BASE_URL =
  process.env.API_BASE_URL ??
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  "http://localhost:8080";

class ApiError extends Error {
  constructor(
    message: string,
    public status?: number,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    headers: { "Content-Type": "application/json" },
    cache: "no-store",
    ...init,
  });

  if (!res.ok) {
    throw new ApiError(`Request to ${path} failed`, res.status);
  }

  return (await res.json()) as T;
}

type PaginatedResponse<T> = {
  content: T[];
};

type BackendPayment = {
  id?: number;
  paymentId?: number;
  amount: string | number;
  currency: string;
  sourceAccount: string;
  destinationAccount: string;
  status: string;
  paymentReference?: string;
  reference?: string;
  idempotencyKey?: string;
  errorCode?: string;
  createdAt?: string;
  updatedAt?: string;
  errorMessage?: string;
};

type BackendPaymentHistory = {
  id: number;
  paymentId: number;
  oldStatus?: string;
  newStatus: string;
  changedAt: string;
  reason?: string;
};

type DashboardResponse = {
  paymentsToday?: number;
  successRate?: number;
};

type MetricsResponse = {
  totalPayments?: number;
  successfulPayments?: number;
  failedPayments?: number;
};

function normalizePayment(payment: BackendPayment): Payment {
  return {
    id: payment.id ?? payment.paymentId ?? 0,
    amount: String(payment.amount ?? "0"),
    currency: payment.currency ?? "INR",
    sourceAccount: payment.sourceAccount ?? "N/A",
    destinationAccount: payment.destinationAccount ?? "N/A",
    reference: payment.reference ?? payment.paymentReference,
    status: payment.status ?? "PENDING",
    errorCode: payment.errorCode,
    idempotencyKey: payment.idempotencyKey ?? "N/A",
    createdAt: payment.createdAt ?? new Date().toISOString(),
    updatedAt:
      payment.updatedAt ?? payment.createdAt ?? new Date().toISOString(),
  };
}

/**
 * Fetches recent payments from the backend.
 * Falls back to demo data if the API is unreachable (e.g. during
 * local frontend-only development before controllers are wired up).
 */
export async function getPayments(): Promise<Payment[]> {
  try {
    const response = await request<
      PaginatedResponse<BackendPayment> | BackendPayment[]
    >("/payments");
    const rows = Array.isArray(response) ? response : response.content;
    return rows.map(normalizePayment);
  } catch {
    return mockPayments;
  }
}

export async function createPayment(
  input: CreatePaymentInput,
): Promise<Payment> {
  const idempotencyKey =
    input.idempotencyKey ??
    (typeof crypto !== "undefined" && "randomUUID" in crypto
      ? crypto.randomUUID()
      : `idem-${Date.now()}`);

  const payload = {
    amount: input.amount,
    currency: input.currency,
    sourceAccount: input.sourceAccount,
    destinationAccount: input.destinationAccount,
    originCountry: input.originCountry,
    destinationCountry: input.destinationCountry,
    idempotencyKey,
  };

  const created = await request<BackendPayment>("/payments", {
    method: "POST",
    body: JSON.stringify(payload),
  });

  return normalizePayment(created);
}

export async function getPaymentById(id: number): Promise<Payment> {
  const payment = await request<BackendPayment>(`/payments/${id}`);
  return normalizePayment(payment);
}

export async function retryPayment(id: number): Promise<Payment> {
  const payment = await request<BackendPayment>(`/payments/${id}/retry`, {
    method: "POST",
  });
  return normalizePayment(payment);
}

export async function cancelPayment(id: number): Promise<Payment> {
  const payment = await request<BackendPayment>(`/payments/${id}/cancel`, {
    method: "POST",
  });
  return normalizePayment(payment);
}

export async function getPaymentHistory(
  id: number,
): Promise<PaymentStatusHistoryEntry[]> {
  const history = await request<BackendPaymentHistory[]>(
    `/payments/${id}/history`,
  );
  return history.map((row) => ({
    id: row.id,
    paymentId: row.paymentId,
    oldStatus: row.oldStatus,
    newStatus: row.newStatus,
    changedAt: row.changedAt,
    reason: row.reason,
  }));
}

export async function getAccounts(): Promise<Account[]> {
  try {
    return await request<Account[]>("/accounts");
  } catch {
    const payments = await getPayments();
    if (payments.length === 0) {
      return mockAccounts;
    }

    const accountNumbers = new Set<string>();
    for (const payment of payments) {
      accountNumbers.add(payment.sourceAccount);
      accountNumbers.add(payment.destinationAccount);
    }

    return Array.from(accountNumbers).map((accountNumber, index) => ({
      id: index + 1,
      accountNumber,
      accountHolder: "Unknown Holder",
      accountType: "CURRENT",
      currency: "INR",
      bankName: "RupeeX Core",
      status: "ACTIVE",
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }));
  }
}

export async function getDashboardStats(): Promise<DashboardStats> {
  try {
    const [dashboard, metrics] = await Promise.all([
      request<DashboardResponse>("/dashboard"),
      request<MetricsResponse>("/metrics"),
    ]);

    const totalPayments = dashboard.paymentsToday ?? metrics.totalPayments ?? 0;

    const successRateRaw =
      dashboard.successRate ??
      (metrics.totalPayments && metrics.totalPayments > 0
        ? ((metrics.successfulPayments ?? 0) / metrics.totalPayments) * 100
        : 0);

    const successRate =
      successRateRaw > 1 ? successRateRaw / 100 : successRateRaw;

    return {
      totalVolume: totalPayments,
      totalPayments,
      successRate,
      activeAccounts: 0,
    };
  } catch {
    return mockStats;
  }
}

export async function getEvents(): Promise<SystemEvent[]> {
  return request<SystemEvent[]>("/events");
}

export async function getFraudRules(): Promise<FraudRule[]> {
  return request<FraudRule[]>("/fraud/rules");
}

export async function createFraudRule(input: FraudRuleInput): Promise<FraudRule> {
  return request<FraudRule>("/fraud/rules", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export async function updateFraudRule(
  id: number,
  input: FraudRuleInput,
): Promise<FraudRule> {
  return request<FraudRule>(`/fraud/rules/${id}`, {
    method: "PUT",
    body: JSON.stringify(input),
  });
}

export async function deleteFraudRule(id: number): Promise<void> {
  await request(`/fraud/rules/${id}`, {
    method: "DELETE",
  });
}

export async function getDeadLetterQueue(): Promise<DeadLetterEntry[]> {
  try {
    return await request<DeadLetterEntry[]>("/dlq");
  } catch {
    return [];
  }
}

export { ApiError };
