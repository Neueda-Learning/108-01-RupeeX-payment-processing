"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { getPayments } from "@/lib/api";
import type { Payment } from "@/lib/types";
import { formatCurrency, formatDateTime } from "@/lib/format";
import { StatusBadge } from "@/components/status-badge";

function totalAmount(rows: Payment[]): number {
  return rows.reduce((sum, row) => {
    const value = Number.parseFloat(row.amount);
    return sum + (Number.isFinite(value) ? value : 0);
  }, 0);
}

export default function AdminViewPage() {
  const [payments, setPayments] = useState<Payment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    getPayments()
      .then((rows) => {
        if (!cancelled) {
          setPayments(rows);
          setError(null);
        }
      })
      .catch((loadError) => {
        if (!cancelled) {
          setError(
            loadError instanceof Error
              ? loadError.message
              : "Unable to load payments",
          );
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
  }, []);

  const successCount = useMemo(
    () =>
      payments.filter((payment) =>
        ["SETTLED", "SUCCESS", "COMPLETED"].includes(payment.status),
      ).length,
    [payments],
  );

  return (
    <div className="mx-auto max-w-7xl space-y-6 px-6 py-10 lg:px-8">
      <header className="space-y-2">
        <p className="text-sm font-semibold uppercase tracking-wide text-orange-700">
          Admin View
        </p>
        <h1 className="text-3xl font-semibold tracking-tight text-slate-900">
          Platform-wide payment operations
        </h1>
        <p className="text-slate-600">
          This screen is for operations admins monitoring all accounts and all
          payment states.
        </p>
      </header>

      {error && (
        <p className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </p>
      )}

      <section className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <article className="panel rounded-2xl p-4">
          <p className="text-xs uppercase tracking-wide text-slate-500">
            Total Payments
          </p>
          <p className="mt-1 text-2xl font-semibold text-slate-900">
            {payments.length.toLocaleString("en-IN")}
          </p>
        </article>
        <article className="panel rounded-2xl p-4">
          <p className="text-xs uppercase tracking-wide text-slate-500">
            Total Processed Value
          </p>
          <p className="mt-1 text-2xl font-semibold text-slate-900">
            {formatCurrency(totalAmount(payments), "INR")}
          </p>
        </article>
        <article className="panel rounded-2xl p-4">
          <p className="text-xs uppercase tracking-wide text-slate-500">
            Settled / Successful
          </p>
          <p className="mt-1 text-2xl font-semibold text-slate-900">
            {successCount.toLocaleString("en-IN")}
          </p>
        </article>
      </section>

      <section className="panel overflow-hidden rounded-2xl">
        <div className="border-b border-black/5 px-6 py-4">
          <h2 className="text-lg font-semibold text-slate-900">
            All payments (admin list)
          </h2>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-black/5">
            <thead className="bg-slate-50">
              <tr>
                {[
                  "Payment",
                  "Source Account",
                  "Destination Account",
                  "Amount",
                  "Status",
                  "Updated",
                  "Action",
                ].map((header) => (
                  <th
                    key={header}
                    className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500"
                  >
                    {header}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-black/5">
              {loading ? (
                <tr>
                  <td
                    colSpan={7}
                    className="px-6 py-8 text-center text-sm text-slate-500"
                  >
                    Loading payments...
                  </td>
                </tr>
              ) : payments.length === 0 ? (
                <tr>
                  <td
                    colSpan={7}
                    className="px-6 py-8 text-center text-sm text-slate-500"
                  >
                    No payments found.
                  </td>
                </tr>
              ) : (
                payments.map((payment) => (
                  <tr key={payment.id}>
                    <td className="px-6 py-4 text-sm font-medium text-slate-900">
                      {payment.reference ?? `Payment #${payment.id}`}
                    </td>
                    <td className="px-6 py-4 text-sm text-slate-700">
                      {payment.sourceAccount}
                    </td>
                    <td className="px-6 py-4 text-sm text-slate-700">
                      {payment.destinationAccount}
                    </td>
                    <td className="px-6 py-4 text-sm font-semibold text-slate-900">
                      {formatCurrency(payment.amount, payment.currency)}
                    </td>
                    <td className="px-6 py-4">
                      <StatusBadge status={payment.status} />
                    </td>
                    <td className="px-6 py-4 text-sm text-slate-500">
                      {formatDateTime(payment.updatedAt)}
                    </td>
                    <td className="px-6 py-4 text-sm">
                      <Link
                        href={`/payments/${payment.id}`}
                        className="font-medium text-orange-700 hover:text-orange-800"
                      >
                        Open payment
                      </Link>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
