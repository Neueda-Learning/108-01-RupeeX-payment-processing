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

/**
 * Base URL of the RupeeX Spring Boot backend, resolved server-side.
 *
 * `API_BASE_URL` (no NEXT_PUBLIC_ prefix) is read at runtime on the
 * server/container and is the preferred variable — it lets the same
 * built image point at different backends (e.g. the `app` service name
 * inside docker-compose) without rebuilding. `NEXT_PUBLIC_API_BASE_URL`
 * is kept as an optional override for local `npm run dev` convenience.
 *
 * The backend is served under the `/api` context path. In production an
 * nginx reverse proxy sits in front of both the frontend and backend
 * containers on a single port, forwarding `/api/*` to the backend and
 * everything else to the frontend — so the browser fallback below is a
 * same-origin relative path, not a hardcoded host:port.
 */
export const API_BASE_URL =
  process.env.API_BASE_URL ??
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  (typeof window !== "undefined" ? "/api" : "http://localhost:8080/api");

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
    let detail = `Request to ${path} failed`;
    try {
      const payload = (await res.json()) as {
        detail?: string;
        message?: string;
      };
      detail = payload.detail ?? payload.message ?? detail;
    } catch {
      // Ignore parse error and keep fallback message.
    }
    throw new ApiError(detail, res.status);
  }

  return (await res.json()) as T;
}

type PaginatedResponse<T> = {
  content: T[];
};

type BackendFraudResult = {
  id?: number;
  paymentId?: number;
  ruleId?: number;
  ruleName?: string;
  triggered?: boolean;
  scoreContribution?: number;
  reason?: string;
};

type BackendRiskScore = {
  id?: number;
  paymentId?: number;
  score?: number;
  category?: string;
  explanation?: string;
  decision?: string;
  createdAt?: string;
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
  riskScore?: BackendRiskScore;
  fraudResults?: BackendFraudResult[];
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
    errorMessage: payment.errorMessage,
    idempotencyKey: payment.idempotencyKey ?? "N/A",
    createdAt: payment.createdAt ?? new Date().toISOString(),
    updatedAt:
      payment.updatedAt ?? payment.createdAt ?? new Date().toISOString(),
    riskScore: payment.riskScore
      ? {
          id: payment.riskScore.id ?? 0,
          paymentId: payment.riskScore.paymentId ?? 0,
          score: payment.riskScore.score ?? 0,
          category: payment.riskScore.category ?? "UNKNOWN",
          explanation: payment.riskScore.explanation ?? "",
          decision: payment.riskScore.decision ?? "Auto Process",
          createdAt: payment.riskScore.createdAt ?? new Date().toISOString(),
        }
      : undefined,
    fraudResults: payment.fraudResults
      ? payment.fraudResults.map((fr) => ({
          id: fr.id ?? 0,
          paymentId: fr.paymentId ?? 0,
          ruleId: fr.ruleId ?? 0,
          ruleName: fr.ruleName ?? "Unknown Rule",
          triggered: fr.triggered ?? false,
          scoreContribution: fr.scoreContribution ?? 0,
          reason: fr.reason ?? "",
        }))
      : undefined,
  };
}

/**
 * Fetches recent payments from the backend.
 */
export async function getPayments(): Promise<Payment[]> {
  const response = await request<
    PaginatedResponse<BackendPayment> | BackendPayment[]
  >("/payments");
  const rows = Array.isArray(response) ? response : response.content;
  return rows.map(normalizePayment);
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
  return await request<Account[]>("/accounts");
}

export async function getDashboardStats(): Promise<DashboardStats> {
  const [dashboard, metrics, payments] = await Promise.all([
    request<DashboardResponse>("/dashboard"),
    request<MetricsResponse>("/metrics"),
    getPayments(),
  ]);

  const totalPayments = dashboard.paymentsToday ?? metrics.totalPayments ?? 0;

  const successRateRaw =
    dashboard.successRate ??
    (metrics.totalPayments && metrics.totalPayments > 0
      ? ((metrics.successfulPayments ?? 0) / metrics.totalPayments) * 100
      : 0);

  const successRate =
    successRateRaw > 1 ? successRateRaw / 100 : successRateRaw;
  const totalVolume = payments.reduce((sum, payment) => {
    const value = Number.parseFloat(payment.amount);
    return sum + (Number.isFinite(value) ? value : 0);
  }, 0);

  return {
    totalVolume,
    totalPayments,
    successRate,
    activeAccounts: new Set(payments.map((payment) => payment.sourceAccount))
      .size,
  };
}

export async function getEvents(): Promise<SystemEvent[]> {
  return request<SystemEvent[]>("/events");
}

export async function getFraudRules(): Promise<FraudRule[]> {
  return request<FraudRule[]>("/fraud/rules");
}

export async function createFraudRule(
  input: FraudRuleInput,
): Promise<FraudRule> {
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
  return request<DeadLetterEntry[]>("/dlq");
}

export { ApiError };
