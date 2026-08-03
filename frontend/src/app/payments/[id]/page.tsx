"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import {
  cancelPayment,
  getPaymentById,
  getPaymentHistory,
  retryPayment,
} from "@/lib/api";
import type { Payment, PaymentStatusHistoryEntry } from "@/lib/types";
import { formatCurrency, formatDateTime } from "@/lib/format";
import { StatusBadge } from "@/components/status-badge";

/** Ordered pipeline steps every payment moves through */
const PIPELINE: { status: string; label: string; description: string }[] = [
  { status: "CREATED",        label: "Created",         description: "Payment request received and recorded" },
  { status: "VALIDATED",      label: "Validated",       description: "Amount, currency, and account details verified" },
  { status: "RISK_ANALYZED",  label: "Risk Analysis",   description: "Transaction scored against risk thresholds" },
  { status: "FRAUD_CHECKED",  label: "Fraud Check",     description: "Fraud detection rules evaluated" },
  { status: "QUEUED",         label: "Queued",          description: "Accepted by processing queue, awaiting worker" },
  { status: "PROCESSING",     label: "Processing",      description: "Worker picked up payment, executing transfer" },
  { status: "SENT",           label: "Sent",            description: "Funds dispatched to destination network" },
  { status: "SETTLED",        label: "Settled",         description: "Payment confirmed and fully settled" },
];

const TERMINAL_FAILED: { status: string; label: string; description: string } = {
  status: "FAILED", label: "Failed", description: "Payment could not be completed",
};
const TERMINAL_CANCELLED: { status: string; label: string; description: string } = {
  status: "CANCELLED", label: "Cancelled", description: "Payment was cancelled before settlement",
};

function getStepState(
  stepStatus: string,
  currentStatus: string,
  reachedStatuses: Set<string>,
): "done" | "current" | "pending" | "skipped" {
  if (reachedStatuses.has(stepStatus)) return "done";
  if (stepStatus === currentStatus) return "current";
  const pipelineIdx = PIPELINE.findIndex((s) => s.status === stepStatus);
  const currentIdx = PIPELINE.findIndex((s) => s.status === currentStatus);
  if (pipelineIdx < currentIdx) return "done";
  return "pending";
}

export default function PaymentDetailPage() {
  const params = useParams<{ id: string }>();
  const paymentId = useMemo(() => Number(params.id), [params.id]);
  const hasValidId = Number.isFinite(paymentId);

  const [payment, setPayment] = useState<Payment | null>(null);
  const [history, setHistory] = useState<PaymentStatusHistoryEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [busyAction, setBusyAction] = useState<"retry" | "cancel" | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!hasValidId) return;
    let cancelled = false;
    Promise.all([getPaymentById(paymentId), getPaymentHistory(paymentId)])
      .then(([paymentData, historyData]) => {
        if (!cancelled) { setPayment(paymentData); setHistory(historyData); }
      })
      .catch((e) => { if (!cancelled) setError(e instanceof Error ? e.message : "Failed to load payment"); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [hasValidId, paymentId]);

  const runAction = async (action: "retry" | "cancel") => {
    setError(null);
    setBusyAction(action);
    try {
      const updated = action === "retry" ? await retryPayment(paymentId) : await cancelPayment(paymentId);
      setPayment(updated);
      setHistory(await getPaymentHistory(paymentId));
    } catch (e) {
      setError(e instanceof Error ? e.message : "Action failed");
    } finally {
      setBusyAction(null);
    }
  };

  // Build set of all statuses that were historically reached
  const reachedStatuses = useMemo(
    () => new Set(history.map((h) => String(h.newStatus))),
    [history],
  );

  const isFailed = payment?.status === "FAILED";
  const isCancelled = payment?.status === "CANCELLED";
  const isTerminal = isFailed || isCancelled || payment?.status === "SETTLED";

  // Find the history entry for each pipeline step (for timestamps)
  const historyByStatus = useMemo(() => {
    const map: Record<string, PaymentStatusHistoryEntry> = {};
    for (const h of history) map[String(h.newStatus)] = h;
    return map;
  }, [history]);

  if (!hasValidId) return (
    <div className="mx-auto max-w-5xl space-y-4 px-6 py-10 lg:px-8">
      <p className="text-sm text-red-600">Invalid payment id.</p>
      <Link href="/payments" className="text-sm font-medium text-orange-700">Back to payments</Link>
    </div>
  );

  if (loading) return (
    <div className="mx-auto max-w-5xl px-6 py-10 lg:px-8">
      <p className="text-sm text-slate-500">Loading payment…</p>
    </div>
  );

  if (!payment) return (
    <div className="mx-auto max-w-5xl space-y-4 px-6 py-10 lg:px-8">
      <p className="text-sm text-red-600">Payment not found.</p>
      <Link href="/payments" className="text-sm font-medium text-orange-700">Back to payments</Link>
    </div>
  );

  return (
    <div className="mx-auto max-w-5xl space-y-8 px-6 py-10 lg:px-8">
      {/* Header */}
      <header className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <Link href="/payments" className="text-xs font-medium text-orange-700 hover:text-orange-800">
            ← Back to payments
          </Link>
          <p className="mt-2 text-sm font-semibold uppercase tracking-wide text-orange-700">Payment Detail</p>
          <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-900">
            {payment.reference ?? `Payment #${payment.id}`}
          </h1>
          <p className="mt-1 font-mono text-xs text-slate-400">{payment.idempotencyKey}</p>
        </div>
        <div className="flex items-center gap-2 shrink-0">
          <button
            onClick={() => runAction("retry")}
            disabled={busyAction !== null}
            className="rounded-lg border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-60"
          >
            {busyAction === "retry" ? "Retrying…" : "Retry"}
          </button>
          <button
            onClick={() => runAction("cancel")}
            disabled={busyAction !== null || isTerminal}
            className="rounded-lg bg-red-600 px-4 py-2 text-sm font-semibold text-white hover:bg-red-700 disabled:opacity-60"
          >
            {busyAction === "cancel" ? "Cancelling…" : "Cancel"}
          </button>
        </div>
      </header>

      {error && (
        <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>
      )}

      {/* Summary cards */}
      <section className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <div className="panel rounded-2xl p-4">
          <p className="text-xs text-slate-500">Amount</p>
          <p className="mt-1 text-xl font-bold text-slate-900">
            {formatCurrency(payment.amount, payment.currency)}
          </p>
        </div>
        <div className="panel rounded-2xl p-4">
          <p className="text-xs text-slate-500">Status</p>
          <div className="mt-2"><StatusBadge status={payment.status} /></div>
        </div>
        <div className="panel rounded-2xl p-4">
          <p className="text-xs text-slate-500">Source</p>
          <p className="mt-1 font-mono text-sm font-semibold text-slate-900">{payment.sourceAccount}</p>
        </div>
        <div className="panel rounded-2xl p-4">
          <p className="text-xs text-slate-500">Destination</p>
          <p className="mt-1 font-mono text-sm font-semibold text-slate-900">{payment.destinationAccount}</p>
        </div>
      </section>

      {/* Pipeline stepper */}
      <section className="panel rounded-2xl p-6">
        <h2 className="text-lg font-semibold text-slate-900">Payment pipeline</h2>
        <p className="mt-0.5 text-sm text-slate-500">
          Each payment moves through these stages. Fraud and risk rules are evaluated at the highlighted steps.
        </p>

        <ol className="mt-6 space-y-0">
          {PIPELINE.map((step, idx) => {
            const state = getStepState(step.status, payment.status, reachedStatuses);
            const histEntry = historyByStatus[step.status];
            const isLast = idx === PIPELINE.length - 1;

            return (
              <li key={step.status} className="flex gap-4">
                {/* Connector line + icon */}
                <div className="flex flex-col items-center">
                  <div
                    className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full border-2 text-xs font-bold transition-colors ${
                      state === "done"
                        ? "border-emerald-500 bg-emerald-500 text-white"
                        : state === "current"
                          ? "border-orange-500 bg-orange-500 text-white"
                          : "border-slate-200 bg-white text-slate-300"
                    }`}
                  >
                    {state === "done" ? "✓" : idx + 1}
                  </div>
                  {!isLast && (
                    <div
                      className={`w-0.5 flex-1 min-h-[20px] ${
                        state === "done" ? "bg-emerald-200" : "bg-slate-100"
                      }`}
                    />
                  )}
                </div>

                {/* Step content */}
                <div className={`pb-6 ${isLast ? "pb-0" : ""}`}>
                  <div className="flex flex-wrap items-center gap-2">
                    <span
                      className={`text-sm font-semibold ${
                        state === "current"
                          ? "text-orange-700"
                          : state === "done"
                            ? "text-slate-900"
                            : "text-slate-400"
                      }`}
                    >
                      {step.label}
                    </span>
                    {(step.status === "RISK_ANALYZED" || step.status === "FRAUD_CHECKED") && (
                      <span className="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-700">
                        {step.status === "RISK_ANALYZED" ? "Risk rules applied" : "Fraud rules evaluated"}
                      </span>
                    )}
                    {state === "current" && (
                      <span className="rounded-full bg-orange-100 px-2 py-0.5 text-xs font-medium text-orange-700">
                        Current
                      </span>
                    )}
                    {histEntry && (
                      <span className="text-xs text-slate-400">
                        {formatDateTime(histEntry.changedAt)}
                      </span>
                    )}
                  </div>
                  <p className={`mt-0.5 text-xs ${state === "pending" ? "text-slate-300" : "text-slate-500"}`}>
                    {step.description}
                  </p>
                  {histEntry?.reason && state !== "pending" && (
                    <p className="mt-1 text-xs text-slate-400 italic">{histEntry.reason}</p>
                  )}
                </div>
              </li>
            );
          })}

          {/* Terminal: failed or cancelled */}
          {(isFailed || isCancelled) && (() => {
            const terminal = isFailed ? TERMINAL_FAILED : TERMINAL_CANCELLED;
            const histEntry = historyByStatus[terminal.status];
            return (
              <li key={terminal.status} className="flex gap-4 mt-0">
                <div className="flex flex-col items-center">
                  <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full border-2 border-red-400 bg-red-400 text-white text-xs font-bold">
                    ✕
                  </div>
                </div>
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="text-sm font-semibold text-red-700">{terminal.label}</span>
                    {histEntry && (
                      <span className="text-xs text-slate-400">{formatDateTime(histEntry.changedAt)}</span>
                    )}
                  </div>
                  <p className="mt-0.5 text-xs text-slate-500">{terminal.description}</p>
                  {payment.errorCode && (
                    <p className="mt-1 text-xs font-mono text-red-500">{payment.errorCode}</p>
                  )}
                  {payment.errorMessage && (
                    <p className="mt-0.5 text-xs text-red-400">{payment.errorMessage}</p>
                  )}
                  {histEntry?.reason && (
                    <p className="mt-0.5 text-xs text-slate-400 italic">{histEntry.reason}</p>
                  )}
                </div>
              </li>
            );
          })()}
        </ol>
      </section>

      {/* Raw history log */}
      <section className="panel rounded-2xl p-6">
        <h2 className="text-base font-semibold text-slate-900">Full status log</h2>
        <p className="mt-0.5 mb-4 text-xs text-slate-500">All recorded transitions, newest first</p>
        {history.length === 0 ? (
          <p className="text-sm text-slate-400">No history records.</p>
        ) : (
          <div className="space-y-2">
            {[...history].reverse().map((row) => (
              <div key={row.id} className="flex flex-wrap items-center gap-3 rounded-lg border border-black/5 px-4 py-2.5 text-sm">
                <span className="text-slate-400 text-xs">{formatDateTime(row.changedAt)}</span>
                <span className="text-slate-500 text-xs">{row.oldStatus ?? "—"}</span>
                <span className="text-slate-300">→</span>
                <StatusBadge status={String(row.newStatus)} />
                {row.reason && <span className="text-xs text-slate-400 italic">{row.reason}</span>}
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

