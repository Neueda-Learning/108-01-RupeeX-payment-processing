import type { Account, DashboardStats, Payment } from "./types";

/**
 * Demo/mock data used to render the dashboard before the backend
 * REST endpoints are available. Replace with real API calls in
 * `src/lib/api.ts` once `com.rupeex.main.controller` exposes them.
 */

export const mockPayments: Payment[] = [
  {
    id: 1001,
    amount: "125000.00",
    currency: "INR",
    sourceAccount: "ACC-10293",
    destinationAccount: "ACC-58213",
    reference: "Invoice #INV-4471",
    status: "COMPLETED",
    idempotencyKey: "a1f7c9e2-1",
    createdAt: "2026-07-30T09:12:00",
    updatedAt: "2026-07-30T09:12:41",
  },
  {
    id: 1002,
    amount: "42000.50",
    currency: "INR",
    sourceAccount: "ACC-77410",
    destinationAccount: "ACC-10293",
    reference: "Vendor payout",
    status: "PROCESSING",
    idempotencyKey: "a1f7c9e2-2",
    createdAt: "2026-07-30T10:03:12",
    updatedAt: "2026-07-30T10:03:12",
  },
  {
    id: 1003,
    amount: "980000.00",
    currency: "INR",
    sourceAccount: "ACC-58213",
    destinationAccount: "ACC-92110",
    reference: "Payroll batch",
    status: "PENDING",
    idempotencyKey: "a1f7c9e2-3",
    createdAt: "2026-07-30T11:45:22",
    updatedAt: "2026-07-30T11:45:22",
  },
  {
    id: 1004,
    amount: "15999.99",
    currency: "USD",
    sourceAccount: "ACC-33012",
    destinationAccount: "ACC-77410",
    reference: "Refund - order #8823",
    status: "FAILED",
    errorCode: "INSUFFICIENT_FUNDS",
    idempotencyKey: "a1f7c9e2-4",
    createdAt: "2026-07-29T18:22:05",
    updatedAt: "2026-07-29T18:22:19",
  },
  {
    id: 1005,
    amount: "5400.00",
    currency: "INR",
    sourceAccount: "ACC-10293",
    destinationAccount: "ACC-33012",
    reference: "Subscription renewal",
    status: "COMPLETED",
    idempotencyKey: "a1f7c9e2-5",
    createdAt: "2026-07-29T14:02:51",
    updatedAt: "2026-07-29T14:03:02",
  },
];

export const mockAccounts: Account[] = [
  {
    id: 1,
    accountNumber: "ACC-10293",
    accountHolder: "Aarav Mehta",
    accountType: "SAVINGS",
    currency: "INR",
    bankName: "HDFC Bank",
    ifscCode: "HDFC0001234",
    status: "ACTIVE",
    createdAt: "2026-01-12T08:00:00",
    updatedAt: "2026-07-30T09:12:41",
  },
  {
    id: 2,
    accountNumber: "ACC-58213",
    accountHolder: "Priya Sharma",
    accountType: "CURRENT",
    currency: "INR",
    bankName: "ICICI Bank",
    ifscCode: "ICIC0005678",
    status: "ACTIVE",
    createdAt: "2026-02-03T08:00:00",
    updatedAt: "2026-07-30T11:45:22",
  },
  {
    id: 3,
    accountNumber: "ACC-77410",
    accountHolder: "Vendor Services Pvt Ltd",
    accountType: "CURRENT",
    currency: "INR",
    bankName: "Axis Bank",
    ifscCode: "UTIB0009988",
    status: "ACTIVE",
    createdAt: "2026-03-18T08:00:00",
    updatedAt: "2026-07-30T10:03:12",
  },
  {
    id: 4,
    accountNumber: "ACC-33012",
    accountHolder: "Global Traders Inc.",
    accountType: "CURRENT",
    currency: "USD",
    bankName: "Citibank",
    swiftCode: "CITIUS33",
    status: "SUSPENDED",
    createdAt: "2026-04-22T08:00:00",
    updatedAt: "2026-07-29T18:22:19",
  },
];

export const mockStats: DashboardStats = {
  totalVolume: 1168400.49,
  totalPayments: mockPayments.length,
  successRate: 0.94,
  activeAccounts: mockAccounts.filter((a) => a.status === "ACTIVE").length,
};
