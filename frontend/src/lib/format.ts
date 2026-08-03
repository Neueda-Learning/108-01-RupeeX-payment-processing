import type { PaymentStatus } from "./types";

export function formatCurrency(
  amount: string | number,
  currency: string,
): string {
  const value = typeof amount === "string" ? Number.parseFloat(amount) : amount;
  try {
    return new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency,
      maximumFractionDigits: 2,
    }).format(value);
  } catch {
    return `${currency} ${value.toFixed(2)}`;
  }
}

export function formatDateTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("en-IN", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

export function formatPercent(value: number): string {
  return new Intl.NumberFormat("en-IN", {
    style: "percent",
    maximumFractionDigits: 1,
  }).format(value);
}

const STATUS_STYLES: Record<string, string> = {
  CREATED:
    "bg-slate-500/10 text-slate-600 dark:text-slate-400 ring-slate-500/20",
  VALIDATED:
    "bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 ring-indigo-500/20",
  RISK_ANALYZED:
    "bg-violet-500/10 text-violet-600 dark:text-violet-400 ring-violet-500/20",
  FRAUD_CHECKED:
    "bg-cyan-500/10 text-cyan-600 dark:text-cyan-400 ring-cyan-500/20",
  QUEUED: "bg-sky-500/10 text-sky-600 dark:text-sky-400 ring-sky-500/20",
  COMPLETED:
    "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 ring-emerald-500/20",
  PROCESSING:
    "bg-amber-500/10 text-amber-600 dark:text-amber-400 ring-amber-500/20",
  PENDING: "bg-sky-500/10 text-sky-600 dark:text-sky-400 ring-sky-500/20",
  SENT: "bg-blue-500/10 text-blue-600 dark:text-blue-400 ring-blue-500/20",
  SETTLED:
    "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 ring-emerald-500/20",
  SUCCESS:
    "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 ring-emerald-500/20",
  FAILED: "bg-red-500/10 text-red-600 dark:text-red-400 ring-red-500/20",
  CANCELLED:
    "bg-slate-500/10 text-slate-600 dark:text-slate-400 ring-slate-500/20",
  REVERSED:
    "bg-purple-500/10 text-purple-600 dark:text-purple-400 ring-purple-500/20",
  ACTIVE:
    "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 ring-emerald-500/20",
  INACTIVE:
    "bg-slate-500/10 text-slate-600 dark:text-slate-400 ring-slate-500/20",
  SUSPENDED:
    "bg-amber-500/10 text-amber-600 dark:text-amber-400 ring-amber-500/20",
  CLOSED: "bg-red-500/10 text-red-600 dark:text-red-400 ring-red-500/20",
};

export function statusStyles(status: PaymentStatus | string): string {
  return (
    STATUS_STYLES[status] ??
    "bg-slate-500/10 text-slate-600 dark:text-slate-400 ring-slate-500/20"
  );
}
