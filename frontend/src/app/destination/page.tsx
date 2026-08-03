"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { getPayments } from "@/lib/api";
import type { Payment } from "@/lib/types";
import { formatCurrency, formatDateTime } from "@/lib/format";
import { StatusBadge } from "@/components/status-badge";

function sumAmount(rows: Payment[]): number {
  return rows.reduce((sum, row) => {
    const value = Number.parseFloat(row.amount);
    return sum + (Number.isFinite(value) ? value : 0);
  }, 0);
}

export default function DestinationAccountViewPage() {
  const [payments, setPayments] = useState<Payment[]>([]);
  const [selectedAccount, setSelectedAccount] = useState<string>("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    getPayments()
      .then((rows) => {
        if (!cancelled) {
          setPayments(rows);
          setSelectedAccount(rows[0]?.destinationAccount ?? "");
          setError(null);
        }
      })
      .catch((loadError) => {
        if (!cancelled) {
          setError(
            loadError instanceof Error
              ? loadError.message
              : "Unable to load destination account data",
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

  const destinationAccounts = useMemo(
    () =>
      Array.from(
        new Set(payments.map((payment) => payment.destinationAccount)),
      ),
    [payments],
  );

  const rows = useMemo(
    () =>
      selectedAccount
        ? payments.filter(
            (payment) => payment.destinationAccount === selectedAccount,
          )
        : payments,
    [payments, selectedAccount],
  );

  const settledCount = rows.filter((payment) =>
    ["SETTLED", "SUCCESS", "COMPLETED"].includes(payment.status),
  ).length;

  return (
    <div className="mx-auto max-w-7xl space-y-6 px-6 py-10 lg:px-8">
      <header className="space-y-2">
        <p className="text-sm font-semibold uppercase tracking-wide text-emerald-700">
          Destination Account View
        </p>
        <h1 className="text-3xl font-semibold tracking-tight text-slate-900">
          Incoming payment monitoring
        </h1>
        <p className="text-slate-600">
          Use this screen to inspect what a destination account has received and
          track settlement status.
        </p>
      </header>

      {error && (
        <p className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </p>
      )}

      <section className="panel rounded-2xl p-5">
        <label
          className="text-sm font-medium text-slate-700"
          htmlFor="destination-account"
        >
          Select destination account
        </label>
        <select
          id="destination-account"
          className="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 sm:max-w-xl"
          value={selectedAccount}
          onChange={(event) => setSelectedAccount(event.target.value)}
          disabled={destinationAccounts.length === 0}
        >
          {destinationAccounts.length === 0 ? (
            <option value="">No destination accounts available</option>
          ) : (
            destinationAccounts.map((account) => (
              <option key={account} value={account}>
                {account}
              </option>
            ))
          )}
        </select>
      </section>

      <section className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <article className="panel rounded-2xl p-4">
          <p className="text-xs uppercase tracking-wide text-slate-500">
            Incoming Payments
          </p>
          <p className="mt-1 text-2xl font-semibold text-slate-900">
            {rows.length.toLocaleString("en-IN")}
          </p>
        </article>
        <article className="panel rounded-2xl p-4">
          <p className="text-xs uppercase tracking-wide text-slate-500">
            Incoming Amount
          </p>
          <p className="mt-1 text-2xl font-semibold text-slate-900">
            {formatCurrency(sumAmount(rows), "INR")}
          </p>
        </article>
        <article className="panel rounded-2xl p-4">
          <p className="text-xs uppercase tracking-wide text-slate-500">
            Settled / Successful
          </p>
          <p className="mt-1 text-2xl font-semibold text-slate-900">
            {settledCount.toLocaleString("en-IN")}
          </p>
        </article>
      </section>

      <section className="panel overflow-hidden rounded-2xl">
        <div className="border-b border-black/5 px-6 py-4">
          <h2 className="text-lg font-semibold text-slate-900">
            Incoming transactions list
          </h2>
        </div>

        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-black/5">
            <thead className="bg-slate-50">
              <tr>
                {["Payment", "Source Account", "Amount", "Status", "Updated", "Action"].map(
                  (header) => (
                    <th
                      key={header}
                      className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500"
                    >
                      {header}
                    </th>
                  ),
                )}
              </tr>
            </thead>
            <tbody className="divide-y divide-black/5">
              {loading ? (
                <tr>
                  <td colSpan={6} className="px-6 py-8 text-center text-sm text-slate-500">
                    Loading destination account data...
                  </td>
                </tr>
              ) : rows.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-6 py-8 text-center text-sm text-slate-500">
                    No incoming payments found for this destination account.
                  </td>
                </tr>
              ) : (
                rows.map((payment) => (
                  <tr key={payment.id}>
                    <td className="px-6 py-4 text-sm font-medium text-slate-900">
                      {payment.reference ?? `Payment #${payment.id}`}
                    </td>
                    <td className="px-6 py-4 text-sm text-slate-700">
                      {payment.sourceAccount}
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
                        className="font-medium text-emerald-700 hover:text-emerald-800"
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
