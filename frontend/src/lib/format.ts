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
  if (!value) return value;
  // The backend serializes LocalDateTime values that are already IST
  // wall-clock time (e.g. "2026-08-06T15:30:00") with no UTC offset. Parsing
  // that directly with `new Date()` would apply the *browser's* local
  // timezone, silently shifting the displayed time for any viewer outside
  // IST. Instead, parse the wall-clock components manually and render them
  // as UTC so Intl.DateTimeFormat performs no further conversion, then label
  // the result as IST explicitly.
  const match = value.match(/^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2}))?/);
  if (!match) return value;
  const [, year, month, day, hour, minute, second] = match;
  const utcDate = new Date(
    Date.UTC(
      Number(year),
      Number(month) - 1,
      Number(day),
      Number(hour),
      Number(minute),
      Number(second ?? "0"),
    ),
  );
  if (Number.isNaN(utcDate.getTime())) return value;
  return (
    new Intl.DateTimeFormat("en-IN", {
      dateStyle: "medium",
      timeStyle: "short",
      timeZone: "UTC",
    }).format(utcDate) + " IST"
  );
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
  SCHEDULED:
    "bg-purple-500/10 text-purple-600 dark:text-purple-400 ring-purple-500/20",
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
  PENDING_ADMIN_APPROVAL:
    "bg-orange-500/10 text-orange-600 dark:text-orange-400 ring-orange-500/20",
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
