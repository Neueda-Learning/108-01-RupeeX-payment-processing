import type { UserRole } from "./user-store";

export const ONBOARDING_BASE_URL =
  process.env.NEXT_PUBLIC_ONBOARDING_BASE_URL ??
  (typeof window !== "undefined"
    ? `${window.location.protocol}//${window.location.hostname}:8081`
    : "http://localhost:8081");

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
  const customer = await onboardingRequest<OnboardingCustomer>("/onboarding/customers", {
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
  await onboardingRequest(`/onboarding/customers/${id}/consents`, {
    method: "POST",
    body: JSON.stringify({
      consentType: "TERMS_AND_PRIVACY",
      version: "v1.0",
      accepted: true,
    }),
  });

  // Step 3: Submit for review
  await onboardingRequest(`/onboarding/customers/${id}/submit`, { method: "POST" });

  // Step 4: Auto-approve
  await onboardingRequest(`/onboarding/customers/${id}/approve`, { method: "POST" });

  return customer;
}

export async function listOnboardingUsers(): Promise<OnboardingCustomer[]> {
  return onboardingRequest<OnboardingCustomer[]>("/onboarding/customers");
}

