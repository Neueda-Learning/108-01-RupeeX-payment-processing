import type { UserRole } from "./user-store";

/**
 * Base URL of the RupeeX onboarding service, mirroring the pattern in api.ts.
 *
 * `ONBOARDING_BASE_URL` (no NEXT_PUBLIC_ prefix) is read server-side and
 * points directly at the onboarding container over the docker network,
 * already including the /onboarding context path
 * (e.g. http://onboarding-app:8083/onboarding).
 *
 * In the browser the base URL is the same-origin relative path /onboarding so
 * that all requests are routed through the nginx reverse proxy.
 */
export const ONBOARDING_BASE_URL =
  process.env.ONBOARDING_BASE_URL ??
  process.env.NEXT_PUBLIC_ONBOARDING_BASE_URL ??
  (typeof window !== "undefined" ? "/onboarding" : "http://localhost:8081/onboarding");

async function onboardingRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${ONBOARDING_BASE_URL}${path}`, {
    headers: { "Content-Type": "application/json" },
    ...init,
  });
  if (!res.ok) {
    let msg = `Onboarding request to ${path} failed`;
    try {
      const body = (await res.json()) as { message?: string };
      msg = body.message ?? msg;
    } catch {
      // keep fallback
    }
    throw new Error(msg);
  }
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

export interface CreateUserInput {
  fullName: string;
  email: string;
  phone: string;
  dob?: string;
  accountNumber: string;
  accountType: string;
  currency: string;
  countryCode?: string;
  role: UserRole;
}

export interface OnboardingCustomer {
  customerId: string;
  fullName: string;
  email: string;
  phone: string;
  accountNumber: string;
  status: string;
  role: string;
}

export async function createAndApproveUser(input: CreateUserInput): Promise<OnboardingCustomer> {
  // Step 1: Create customer
  const customer = await onboardingRequest<OnboardingCustomer>("/customers", {
    method: "POST",
    body: JSON.stringify({
      fullName: input.fullName,
      email: input.email,
      phone: input.phone,
      dob: input.dob || null,
      accountNumber: input.accountNumber,
      accountType: input.accountType,
      currency: input.currency,
      countryCode: input.countryCode || null,
      role: input.role.toUpperCase(),
    }),
  });

  const id = customer.customerId;

  // Step 2: Auto-accept terms consent
  await onboardingRequest(`/customers/${id}/consents`, {
    method: "POST",
    body: JSON.stringify({
      consentType: "TERMS_AND_PRIVACY",
      version: "v1.0",
      accepted: true,
    }),
  });

  // Step 3: Submit for review
  await onboardingRequest(`/customers/${id}/submit`, { method: "POST" });

  // Step 4: Auto-approve
  await onboardingRequest(`/customers/${id}/approve`, { method: "POST" });

  return customer;
}

export async function listOnboardingUsers(): Promise<OnboardingCustomer[]> {
  return onboardingRequest<OnboardingCustomer[]>("/customers");
}

