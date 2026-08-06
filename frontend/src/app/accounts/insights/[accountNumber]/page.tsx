"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { getAccounts, getPayments } from "@/lib/api";
import type { Account, Payment } from "@/lib/types";
import { formatCurrency, formatDateTime } from "@/lib/format";
import { StatusBadge } from "@/components/status-badge";
import { TransactionCharts } from "@/components/transaction-charts";
import { useUserStore } from "@/lib/user-store";
import type { AppUser, UserRole } from "@/lib/user-store";
import { listOnboardingUsers } from "@/lib/onboarding-api";

type Tab = "sent" | "received";

export default function AccountInsightsPage() {
  const router = useRouter();
  const params = useParams<{ accountNumber: string }>();
  const selectedFromUrl = Array.isArray(params.accountNumber)
    ? params.accountNumber[0]
    : params.accountNumber;

  const [accounts, setAccounts] = useState<Account[]>([]);
  const [payments, setPayments] = useState<Payment[]>([]);
  const [selected, setSelected] = useState<Account | null>(null);
  const [tab, setTab] = useState<Tab>("sent");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const { currentUser, mergeUsers } = useUserStore();
  const isAdmin = !currentUser || currentUser.role === "admin";

  useEffect(() => {
    let cancelled = false;

    Promise.all([getAccounts(), getPayments(), listOnboardingUsers()])
      .then(([accs, pays, customers]) => {
        if (cancelled) return;

        setAccounts(accs);
        setPayments(pays);
        setError(null);

        mergeUsers(
          customers.map((c) => ({
            customerId: c.customerId,
            name: c.fullName,
            email: c.email,
            phone: c.phone,
            accountNumber: c.accountNumber,
            role: c.role.toLowerCase() as UserRole,
          }) satisfies AppUser),
        );

        const permitted = isAdmin
          ? accs
          : accs.filter((a) => a.accountNumber === currentUser?.accountNumber);

        const selectedByRoute = permitted.find(
          (a) => a.accountNumber === selectedFromUrl,
        );
        const fallback = permitted[0] ?? null;
        const resolved = selectedByRoute ?? fallback;

        setSelected(resolved);

        if (resolved && resolved.accountNumber !== selectedFromUrl) {
          router.replace(`/accounts/insights/${resolved.accountNumber}`);
        }
      })
      .catch((e) => {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : "Unable to load insights");
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [currentUser, isAdmin, mergeUsers, router, selectedFromUrl]);

  const visibleAccounts = useMemo(
    () =>
      isAdmin
        ? accounts
        : accounts.filter((a) => a.accountNumber === currentUser?.accountNumber),
    [accounts, currentUser, isAdmin],
  );

  const sent = useMemo(
    () =>
      selected
        ? payments.filter((p) => p.sourceAccount === selected.accountNumber)
        : [],
    [payments, selected],
  );

  const received = useMemo(
    () =>
      selected
        ? payments.filter((p) => p.destinationAccount === selected.accountNumber)
        : [],
    [payments, selected],
  );

  const rows = tab === "sent" ? sent : received;

  if (loading) {
    return (
      <div className="mx-auto max-w-7xl px-6 py-16 text-center text-sm text-slate-500">
        Loading insights...
      </div>
    );
  }

  if (!selected) {
    return (
      <div className="mx-auto max-w-7xl px-6 py-10 lg:px-8">
        <p className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          No account available for insights.
        </p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl space-y-8 px-6 py-10 lg:px-8">
      <header className="flex flex-wrap items-start justify-between gap-3">
        <div className="space-y-1">
          <p className="text-sm font-semibold uppercase tracking-wide text-orange-700">
            Insights
          </p>
          <h1 className="text-3xl font-bold tracking-tight text-slate-900">
            Account analytics
          </h1>
          <p className="text-slate-600">
            Visual trends and transaction breakdown for account {selected.accountNumber}.
          </p>
        </div>
        <Link
          href="/accounts"
          className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
        >
          Back to accounts
        </Link>
      </header>

      {error && (
        <p className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </p>
      )}

      {isAdmin && (
        <section className="panel rounded-2xl p-5">
          <label
            htmlFor="insights-account-select"
            className="mb-1 block text-sm font-medium text-slate-700"
          >
            Select account
          </label>
          <select
            id="insights-account-select"
            className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 sm:max-w-xl"
            value={selected.accountNumber}
            onChange={(e) => {
              const next = visibleAccounts.find(
                (a) => a.accountNumber === e.target.value,
              );
              if (!next) return;
              setSelected(next);
              router.push(`/accounts/insights/${next.accountNumber}`);
            }}
          >
            {visibleAccounts.map((a) => (
              <option key={a.id} value={a.accountNumber}>
                {a.accountNumber} - {a.accountHolder}
              </option>
            ))}
          </select>
        </section>
      )}

      <TransactionCharts
        payments={payments}
        selectedAccountNumber={selected.accountNumber}
      />

      <section className="panel overflow-hidden rounded-2xl">
        <div className="border-b border-black/5 px-6 py-4 flex items-center gap-6">
          <h2 className="text-lg font-semibold text-slate-900 mr-4">
            Transactions
          </h2>
          {(["sent", "received"] as Tab[]).map((t) => (
            <button
              key={t}
              onClick={() => setTab(t)}
              className={`pb-0.5 text-sm font-medium capitalize border-b-2 transition-colors ${
                tab === t
                  ? "border-orange-500 text-orange-700"
                  : "border-transparent text-slate-500 hover:text-slate-700"
              }`}
            >
              {t === "sent" ? `Sent (${sent.length})` : `Received (${received.length})`}
            </button>
          ))}
        </div>

        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-black/5">
            <thead className="bg-slate-50">
              <tr>
                {["Reference", tab === "sent" ? "To" : "From", "Amount", "Status", "Date", ""].map((h, i) => (
                  <th
                    key={i}
                    className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500"
                  >
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-black/5">
              {rows.length === 0 ? (
                <tr>
                  <td
                    colSpan={6}
                    className="px-6 py-10 text-center text-sm text-slate-400"
                  >
                    No {tab} payments for this account.
                  </td>
                </tr>
              ) : (
                rows.map((p) => (
                  <tr key={p.id} className="hover:bg-slate-50/60">
                    <td className="px-6 py-4 text-sm">
                      <p className="font-medium text-slate-900">
                        {p.reference ?? `Payment #${p.id}`}
                      </p>
                      <p className="text-xs text-slate-400 font-mono">
                        {p.idempotencyKey}
                      </p>
                    </td>
                    <td className="px-6 py-4 text-sm text-slate-600">
                      {tab === "sent" ? p.destinationAccount : p.sourceAccount}
                    </td>
                    <td className="px-6 py-4 text-sm font-semibold text-slate-900">
                      {formatCurrency(p.amount, p.currency)}
                    </td>
                    <td className="px-6 py-4">
                      <StatusBadge status={p.status} />
                    </td>
                    <td className="px-6 py-4 text-sm text-slate-500">
                      {formatDateTime(p.createdAt)}
                    </td>
                    <td className="px-6 py-4 text-sm">
                      <Link
                        href={`/payments/${p.id}`}
                        className="font-medium text-orange-700 hover:text-orange-800"
                      >
                        Open
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

