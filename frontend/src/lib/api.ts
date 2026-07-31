import type { Account, DashboardStats, Payment } from "./types";
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

/**
 * Fetches recent payments from the backend.
 * Falls back to demo data if the API is unreachable (e.g. during
 * local frontend-only development before controllers are wired up).
 */
export async function getPayments(): Promise<Payment[]> {
  try {
    return await request<Payment[]>("/api/payments");
  } catch {
    return mockPayments;
  }
}

export async function getAccounts(): Promise<Account[]> {
  try {
    return await request<Account[]>("/api/accounts");
  } catch {
    return mockAccounts;
  }
}

export async function getDashboardStats(): Promise<DashboardStats> {
  try {
    return await request<DashboardStats>("/api/dashboard/stats");
  } catch {
    return mockStats;
  }
}

export { ApiError };
