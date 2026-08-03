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
    if (!hasValidId) {
      return;
    }

    let cancelled = false;
    Promise.all([getPaymentById(paymentId), getPaymentHistory(paymentId)])
      .then(([paymentData, historyData]) => {
        if (!cancelled) {
          setPayment(paymentData);
          setHistory(historyData);
        }
      })
      .catch((loadError) => {
        if (!cancelled) {
          setError(loadError instanceof Error ? loadError.message : "Failed to load payment");
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [hasValidId, paymentId]);

  const runAction = async (action: "retry" | "cancel") => {
    setError(null);
    setBusyAction(action);

    try {
      const updated =
        action === "retry"
          ? await retryPayment(paymentId)
          : await cancelPayment(paymentId);
      setPayment(updated);
      setHistory(await getPaymentHistory(paymentId));
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : "Action failed");
    } finally {
      setBusyAction(null);
    }
  };

  if (!hasValidId) {
    return (
      <div className="mx-auto max-w-5xl space-y-4 px-6 py-10 lg:px-8">
        <p className="text-sm text-red-600 dark:text-red-300">Invalid payment id.</p>
        <Link href="/payments" className="text-sm font-medium text-emerald-700 dark:text-emerald-400">
          Back to payments
        </Link>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="mx-auto max-w-5xl px-6 py-10 lg:px-8">
        <p className="text-sm text-slate-500 dark:text-slate-400">Loading payment...</p>
      </div>
    );
  }

  if (!payment) {
    return (
      <div className="mx-auto max-w-5xl space-y-4 px-6 py-10 lg:px-8">
        <p className="text-sm text-red-600 dark:text-red-300">Payment not found.</p>
        <Link href="/payments" className="text-sm font-medium text-emerald-700 dark:text-emerald-400">
          Back to payments
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-5xl space-y-8 px-6 py-10 lg:px-8">
      <header className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-sm font-semibold uppercase tracking-wide text-emerald-600">
            Payment Detail
          </p>
          <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-900 dark:text-white">
            {payment.reference ?? `Payment #${payment.id}`}
          </h1>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => runAction("retry")}
            disabled={busyAction !== null}
            className="rounded-lg border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-60 dark:border-slate-700 dark:text-slate-200 dark:hover:bg-slate-800"
          >
            {busyAction === "retry" ? "Retrying..." : "Retry"}
          </button>
          <button
            onClick={() => runAction("cancel")}
            disabled={busyAction !== null}
            className="rounded-lg bg-red-600 px-4 py-2 text-sm font-semibold text-white hover:bg-red-700 disabled:opacity-60"
          >
            {busyAction === "cancel" ? "Cancelling..." : "Cancel"}
          </button>
        </div>
      </header>

      {error && (
        <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700 dark:bg-red-500/10 dark:text-red-300">
          {error}
        </p>
      )}

      <section className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div className="rounded-2xl border border-black/5 bg-white p-5 dark:border-white/10 dark:bg-slate-900">
          <p className="text-sm text-slate-500 dark:text-slate-400">Amount</p>
          <p className="mt-1 text-2xl font-semibold text-slate-900 dark:text-white">
            {formatCurrency(payment.amount, payment.currency)}
          </p>
        </div>
        <div className="rounded-2xl border border-black/5 bg-white p-5 dark:border-white/10 dark:bg-slate-900">
          <p className="text-sm text-slate-500 dark:text-slate-400">Status</p>
          <div className="mt-2">
            <StatusBadge status={payment.status} />
          </div>
        </div>
      </section>

      <section className="rounded-2xl border border-black/5 bg-white p-6 dark:border-white/10 dark:bg-slate-900">
        <h2 className="text-lg font-semibold text-slate-900 dark:text-white">
          Lifecycle history
        </h2>
        <div className="mt-4 space-y-3">
          {history.length === 0 ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">No history records found.</p>
          ) : (
            history.map((row) => (
              <div
                key={row.id}
                className="rounded-lg border border-black/5 px-4 py-3 text-sm dark:border-white/10"
              >
                <div className="flex flex-wrap items-center gap-2">
                  <span className="font-medium text-slate-700 dark:text-slate-300">
                    {row.oldStatus ?? "NONE"}
                  </span>
                  <span className="text-slate-400">→</span>
                  <StatusBadge status={String(row.newStatus)} />
                  <span className="text-slate-500 dark:text-slate-400">
                    {formatDateTime(row.changedAt)}
                  </span>
                </div>
                {row.reason && (
                  <p className="mt-1 text-slate-500 dark:text-slate-400">{row.reason}</p>
                )}
              </div>
            ))
          )}
        </div>
      </section>

      <Link
        href="/payments"
        className="inline-flex items-center text-sm font-medium text-emerald-700 hover:text-emerald-800 dark:text-emerald-400 dark:hover:text-emerald-300"
      >
        Back to payments
      </Link>
    </div>
  );
}
