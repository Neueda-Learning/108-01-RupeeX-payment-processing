"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import {
  cancelPayment,
  getDeadLetterQueue,
  getPaymentById,
  retryPayment,
} from "@/lib/api";
import type { DeadLetterEntry, Payment } from "@/lib/types";
import { formatDateTime } from "@/lib/format";

interface EnrichedDLQEntry extends DeadLetterEntry {
  payment?: Payment;
  loading?: boolean;
}

export default function DeadLetterQueuePage() {
  const [entries, setEntries] = useState<EnrichedDLQEntry[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [expandedId, setExpandedId] = useState<number | null>(null);

  const loadEntries = async () => {
    try {
      const dlqEntries = await getDeadLetterQueue();

      // Enrich with payment details
      const enriched: EnrichedDLQEntry[] = dlqEntries.map((entry) => ({
        ...entry,
        loading: true,
      }));
      setEntries(enriched);
      setError(null);

      // Fetch payment details for each entry
      for (let i = 0; i < enriched.length; i++) {
        try {
          const payment = await getPaymentById(enriched[i].paymentId);
          setEntries((prev) => {
            const updated = [...prev];
            updated[i] = { ...updated[i], payment, loading: false };
            return updated;
          });
        } catch {
          setEntries((prev) => {
            const updated = [...prev];
            updated[i] = { ...updated[i], loading: false };
            return updated;
          });
        }
      }
    } catch (loadError) {
      setError(
        loadError instanceof Error ? loadError.message : "Unable to load DLQ",
      );
    }
  };

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void loadEntries();
    }, 0);
    return () => window.clearTimeout(timer);
  }, []);

  const getFailureColor = (errorCode?: string) => {
    if (!errorCode) return "bg-slate-50 border-slate-200";
    if (errorCode.includes("INSUFFICIENT"))
      return "bg-amber-50 border-amber-200";
    if (errorCode.includes("FRAUD") || errorCode.includes("RISK"))
      return "bg-red-50 border-red-200";
    if (errorCode.includes("VALIDATION"))
      return "bg-orange-50 border-orange-200";
    return "bg-slate-50 border-slate-200";
  };

  const getErrorBadgeColor = (errorCode?: string) => {
    if (!errorCode) return "bg-slate-100 text-slate-700";
    if (errorCode.includes("INSUFFICIENT"))
      return "bg-amber-100 text-amber-700";
    if (errorCode.includes("FRAUD") || errorCode.includes("RISK"))
      return "bg-red-100 text-red-700";
    if (errorCode.includes("VALIDATION"))
      return "bg-orange-100 text-orange-700";
    return "bg-slate-100 text-slate-700";
  };

  return (
    <div className="mx-auto max-w-7xl space-y-8 px-6 py-10 lg:px-8">
      <header>
        <p className="text-sm font-semibold uppercase tracking-wide text-orange-700">
          Dead Letter Queue
        </p>
        <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-900">
          Failed payment recovery
        </h1>
        <p className="text-slate-600">
          Inspect payments that exceeded retry limits. View transaction details,
          error information, and take recovery actions.
        </p>
      </header>

      {error && (
        <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </p>
      )}

      <section className="space-y-4">
        {entries.length === 0 ? (
          <div className="panel rounded-2xl px-6 py-8 text-center text-sm text-slate-500">
            No failed payments in queue. All payments are processing
            successfully.
          </div>
        ) : (
          entries.map((entry) => (
            <div
              key={entry.id}
              className={`panel rounded-2xl border-l-4 transition-all ${getFailureColor(entry.payment?.errorCode)}`}
              style={{
                borderLeftColor: entry.payment?.errorCode?.includes(
                  "INSUFFICIENT",
                )
                  ? "#d97706"
                  : entry.payment?.errorCode?.includes("FRAUD")
                    ? "#dc2626"
                    : "#64748b",
              }}
            >
              {/* Header Row */}
              <div className="px-6 py-4">
                <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                  <div className="flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <h3 className="text-lg font-semibold text-slate-900">
                        Payment #{entry.paymentId}
                      </h3>
                      {entry.payment?.errorCode && (
                        <span
                          className={`inline-flex rounded-full px-2 py-1 text-xs font-semibold ${getErrorBadgeColor(entry.payment.errorCode)}`}
                        >
                          {entry.payment.errorCode}
                        </span>
                      )}
                    </div>
                    {entry.payment && (
                      <p className="mt-2 text-sm text-slate-600">
                        <span className="font-semibold">
                          {entry.payment.amount}
                        </span>{" "}
                        {entry.payment.currency}
                        {" • "}
                        {entry.payment.sourceAccount} →{" "}
                        {entry.payment.destinationAccount}
                      </p>
                    )}
                  </div>
                  <button
                    onClick={() =>
                      setExpandedId(expandedId === entry.id ? null : entry.id)
                    }
                    className="rounded-lg bg-slate-100 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-200"
                  >
                    {expandedId === entry.id ? "Hide Details" : "Show Details"}
                  </button>
                </div>
              </div>

              {/* Expanded Details */}
              {expandedId === entry.id && (
                <>
                  <div className="border-t border-black/5 px-6 py-4">
                    <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
                      {/* Transaction Info */}
                      {entry.payment && (
                        <div>
                          <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                            Transaction Details
                          </p>
                          <dl className="mt-3 space-y-2 text-sm">
                            <div className="flex justify-between">
                              <dt className="font-medium text-slate-900">
                                Reference:
                              </dt>
                              <dd className="text-slate-600">
                                {entry.payment.idempotencyKey}
                              </dd>
                            </div>
                            <div className="flex justify-between">
                              <dt className="font-medium text-slate-900">
                                Amount:
                              </dt>
                              <dd className="font-semibold text-slate-900">
                                {entry.payment.amount} {entry.payment.currency}
                              </dd>
                            </div>
                            <div className="flex justify-between">
                              <dt className="font-medium text-slate-900">
                                From:
                              </dt>
                              <dd className="text-slate-600">
                                {entry.payment.sourceAccount}
                              </dd>
                            </div>
                            <div className="flex justify-between">
                              <dt className="font-medium text-slate-900">
                                To:
                              </dt>
                              <dd className="text-slate-600">
                                {entry.payment.destinationAccount}
                              </dd>
                            </div>
                            <div className="flex justify-between">
                              <dt className="font-medium text-slate-900">
                                Created:
                              </dt>
                              <dd className="text-slate-600">
                                {entry.payment.createdAt
                                  ? formatDateTime(entry.payment.createdAt)
                                  : "-"}
                              </dd>
                            </div>
                          </dl>
                        </div>
                      )}

                      {/* Failure Info */}
                      <div>
                        <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                          Failure Information
                        </p>
                        <dl className="mt-3 space-y-2 text-sm">
                          <div className="flex justify-between">
                            <dt className="font-medium text-slate-900">
                              Error Code:
                            </dt>
                            <dd
                              className={`rounded px-2 py-1 ${getErrorBadgeColor(entry.payment?.errorCode)}`}
                            >
                              {entry.payment?.errorCode || "N/A"}
                            </dd>
                          </div>
                          <div className="flex flex-col gap-1">
                            <dt className="font-medium text-slate-900">
                              Error Message:
                            </dt>
                            <dd className="rounded bg-slate-100 px-3 py-2 font-mono text-xs text-slate-700">
                              {entry.payment?.errorMessage || entry.reason}
                            </dd>
                          </div>
                          <div className="flex justify-between">
                            <dt className="font-medium text-slate-900">
                              Retry Attempts:
                            </dt>
                            <dd className="font-semibold text-slate-900">
                              {entry.lastRetryCount}
                            </dd>
                          </div>
                          <div className="flex justify-between">
                            <dt className="font-medium text-slate-900">
                              Queued At:
                            </dt>
                            <dd className="text-slate-600">
                              {entry.createdAt
                                ? formatDateTime(entry.createdAt)
                                : "-"}
                            </dd>
                          </div>
                        </dl>
                      </div>
                    </div>
                  </div>

                   {/* Actions */}
                   <div className="border-t border-black/5 bg-slate-50 px-6 py-4">
                     <div className="flex flex-col gap-2 sm:flex-row">
                       <Link
                         href={`/payments/${entry.paymentId}`}
                         className="rounded-lg border border-blue-300 bg-blue-50 px-4 py-2 font-medium text-blue-700 hover:bg-blue-100 text-center"
                       >
                         📋 View Details
                       </Link>
                       <button
                         onClick={async () => {
                           await retryPayment(entry.paymentId);
                           await loadEntries();
                         }}
                         className="rounded-lg border border-emerald-300 bg-emerald-50 px-4 py-2 font-medium text-emerald-700 hover:bg-emerald-100"
                       >
                         ↻ Retry Payment
                       </button>
                       <button
                         onClick={async () => {
                           if (
                             window.confirm(
                               "Are you sure? This will permanently cancel the payment.",
                             )
                           ) {
                             await cancelPayment(entry.paymentId);
                             await loadEntries();
                           }
                         }}
                         className="rounded-lg bg-red-600 px-4 py-2 font-medium text-white hover:bg-red-700"
                       >
                         ✕ Cancel Payment
                       </button>
                     </div>
                   </div>
                </>
              )}
            </div>
          ))
        )}
      </section>
    </div>
  );
}
