"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { getAccounts, getPayments } from "@/lib/api";
import type { Account, Payment } from "@/lib/types";
import { formatCurrency, formatDateTime } from "@/lib/format";
import { StatusBadge } from "@/components/status-badge";

function pct(n: number, total: number) {
  if (total === 0) return "0%";
  return `${((n / total) * 100).toFixed(1)}%`;
}

function sumAmount(rows: Payment[]): number {
  return rows.reduce((s, p) => s + (Number.parseFloat(p.amount) || 0), 0);
}

const SETTLED_STATES = ["SETTLED", "SUCCESS", "COMPLETED"];
const FAILED_STATES = ["FAILED", "CANCELLED"];
const ACTIVE_STATES = ["CREATED", "VALIDATED", "RISK_ANALYZED", "FRAUD_CHECKED", "QUEUED", "PROCESSING", "SENT"];

function MetricCard({
  label,
  value,
  sub,
  accent,
}: {
  label: string;
  value: string | number;
  sub?: string;
  accent?: string;
}) {
  return (
    <article className="panel rounded-2xl p-5">
      <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
        {label}
      </p>
      <p className={`mt-1 text-2xl font-bold ${accent ?? "text-slate-900"}`}>
        {value}
      </p>
      {sub && <p className="mt-0.5 text-xs text-slate-400">{sub}</p>}
    </article>
  );
}

function MiniBar({ value, max, color }: { value: number; max: number; color: string }) {
  const w = max === 0 ? 0 : Math.round((value / max) * 100);
  return (
    <div className="h-1.5 w-24 rounded-full bg-slate-100 overflow-hidden">
      <div className={`h-full rounded-full ${color}`} style={{ width: `${w}%` }} />
    </div>
  );
}

export default function AdminViewPage() {
  const [payments, setPayments] = useState<Payment[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    Promise.all([getPayments(), getAccounts()])
      .then(([pays, accs]) => {
        if (cancelled) return;
        setPayments(pays);
        setAccounts(accs);
        setError(null);
      })
      .catch((e) => {
        if (!cancelled)
          setError(e instanceof Error ? e.message : "Unable to load data");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => { cancelled = true; };
  }, []);

  const settled = useMemo(
    () => payments.filter((p) => SETTLED_STATES.includes(p.status)), [payments]);
  const failed = useMemo(
    () => payments.filter((p) => FAILED_STATES.includes(p.status)), [payments]);
  const active = useMemo(
    () => payments.filter((p) => ACTIVE_STATES.includes(p.status)), [payments]);

  const accountStats = useMemo(() => accounts.map((acc) => {
    const sent = payments.filter((p) => p.sourceAccount === acc.accountNumber);
    const recv = payments.filter((p) => p.destinationAccount === acc.accountNumber);
    return { acc, sent, recv };
  }), [accounts, payments]);

  const recentPayments = useMemo(() =>
    [...payments]
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      .slice(0, 8),
    [payments]);

  const statusBreakdown = useMemo(() => {
    const counts: Record<string, number> = {};
    for (const p of payments) counts[p.status] = (counts[p.status] ?? 0) + 1;
    return Object.entries(counts).sort((a, b) => b[1] - a[1]);
  }, [payments]);

  return (
    <div className="mx-auto max-w-7xl space-y-8 px-6 py-10 lg:px-8">
      <header className="space-y-1">
        <p className="text-sm font-semibold uppercase tracking-wide text-orange-700">Admin Dashboard</p>
        <h1 className="text-3xl font-bold tracking-tight text-slate-900">Platform-wide operations metrics</h1>
        <p className="text-slate-600">Complete overview of all payments, accounts, and system health across RupeeX.</p>
      </header>

      {error && (
        <p className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>
      )}

      {/* Primary metrics */}
      <section className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <MetricCard label="Total Payments" value={payments.length} />
        <MetricCard label="Total Volume" value={formatCurrency(sumAmount(payments), "INR")} sub="All currencies, approx" />
        <MetricCard label="Success Rate" value={pct(settled.length, payments.length)} accent="text-emerald-700" sub={`${settled.length} settled`} />
        <MetricCard label="Failure Rate" value={pct(failed.length, payments.length)} accent={failed.length > 0 ? "text-red-700" : "text-slate-900"} sub={`${failed.length} failed / cancelled`} />
      </section>

      {/* Secondary metrics */}
      <section className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <MetricCard label="In-flight" value={active.length} sub="Currently processing" />
        <MetricCard label="Settled Value" value={formatCurrency(sumAmount(settled), "INR")} sub="Successfully completed" />
        <MetricCard label="Total Accounts" value={accounts.length} sub={`${accounts.filter((a) => a.status === "ACTIVE").length} active`} />
        <MetricCard label="Avg Payment" value={payments.length ? formatCurrency(sumAmount(payments) / payments.length, "INR") : "—"} sub="Across all transactions" />
      </section>

      {/* Status breakdown + Account activity */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <section className="panel rounded-2xl p-6">
          <h2 className="text-base font-semibold text-slate-900">Payment status breakdown</h2>
          <p className="mt-0.5 mb-4 text-xs text-slate-500">Distribution of all {payments.length} payments by current state</p>
          {loading ? (
            <p className="text-sm text-slate-400">Loading…</p>
          ) : statusBreakdown.length === 0 ? (
            <p className="text-sm text-slate-400">No payments yet.</p>
          ) : (
            <ul className="space-y-3">
              {statusBreakdown.map(([status, count]) => (
                <li key={status} className="flex items-center gap-3">
                  <span className="w-28 shrink-0"><StatusBadge status={status} /></span>
                  <MiniBar
                    value={count}
                    max={payments.length}
                    color={SETTLED_STATES.includes(status) ? "bg-emerald-500" : FAILED_STATES.includes(status) ? "bg-red-400" : "bg-orange-400"}
                  />
                  <span className="text-sm font-medium text-slate-700">{count}</span>
                  <span className="text-xs text-slate-400">{pct(count, payments.length)}</span>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="panel rounded-2xl overflow-hidden">
          <div className="border-b border-black/5 px-6 py-4 flex items-center justify-between">
            <h2 className="text-base font-semibold text-slate-900">Account activity</h2>
            <Link href="/accounts" className="text-xs font-medium text-orange-700 hover:text-orange-800">View profiles →</Link>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-black/5 text-sm">
              <thead className="bg-slate-50">
                <tr>
                  {["Account", "Holder", "Sent", "Received", "Status", ""].map((h, i) => (
                    <th key={i} className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-black/5">
                {loading ? (
                  <tr><td colSpan={6} className="px-4 py-6 text-center text-slate-400">Loading…</td></tr>
                ) : accountStats.length === 0 ? (
                  <tr><td colSpan={6} className="px-4 py-6 text-center text-slate-400">No accounts found.</td></tr>
                ) : (
                  accountStats.map(({ acc, sent, recv }) => (
                    <tr key={acc.id} className="hover:bg-slate-50/50">
                      <td className="px-4 py-3 font-mono text-xs text-slate-700">{acc.accountNumber}</td>
                      <td className="px-4 py-3 font-medium text-slate-900">{acc.accountHolder}</td>
                      <td className="px-4 py-3 text-slate-600">
                        {sent.length}
                        <span className="ml-1 text-xs text-slate-400">({formatCurrency(sumAmount(sent), acc.currency)})</span>
                      </td>
                      <td className="px-4 py-3 text-slate-600">
                        {recv.length}
                        <span className="ml-1 text-xs text-slate-400">({formatCurrency(sumAmount(recv), acc.currency)})</span>
                      </td>
                      <td className="px-4 py-3">
                        <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${acc.status === "ACTIVE" ? "bg-emerald-50 text-emerald-700" : "bg-slate-100 text-slate-500"}`}>
                          {acc.status}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <Link href="/accounts" className="text-xs font-medium text-orange-700 hover:text-orange-800">Profile</Link>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </section>
      </div>

      {/* Recent payments */}
      <section className="panel overflow-hidden rounded-2xl">
        <div className="border-b border-black/5 px-6 py-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-slate-900">Recent payments</h2>
          <Link href="/payments" className="text-sm font-medium text-orange-700 hover:text-orange-800">View all →</Link>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-black/5">
            <thead className="bg-slate-50">
              <tr>
                {["Payment", "Source", "Destination", "Amount", "Status", "Created", ""].map((h, i) => (
                  <th key={i} className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-black/5">
              {loading ? (
                <tr><td colSpan={7} className="px-6 py-8 text-center text-sm text-slate-400">Loading payments…</td></tr>
              ) : recentPayments.length === 0 ? (
                <tr><td colSpan={7} className="px-6 py-8 text-center text-sm text-slate-400">No payments found.</td></tr>
              ) : (
                recentPayments.map((p) => (
                  <tr key={p.id} className="hover:bg-slate-50/60">
                    <td className="px-6 py-4 text-sm font-medium text-slate-900">{p.reference ?? `Payment #${p.id}`}</td>
                    <td className="px-6 py-4 text-sm text-slate-600">{p.sourceAccount}</td>
                    <td className="px-6 py-4 text-sm text-slate-600">{p.destinationAccount}</td>
                    <td className="px-6 py-4 text-sm font-semibold text-slate-900">{formatCurrency(p.amount, p.currency)}</td>
                    <td className="px-6 py-4"><StatusBadge status={p.status} /></td>
                    <td className="px-6 py-4 text-sm text-slate-500">{formatDateTime(p.createdAt)}</td>
                    <td className="px-6 py-4 text-sm">
                      <Link href={`/payments/${p.id}`} className="font-medium text-orange-700 hover:text-orange-800">Open</Link>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>

      {/* Quick links to operations sections */}
      <section className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        {[
          { href: "/rules", label: "Fraud Rules", desc: "Manage detection policies" },
          { href: "/dlq", label: "Dead Letter Queue", desc: "Inspect failed retries" },
          { href: "/events", label: "System Events", desc: "Real-time event stream" },
          { href: "/accounts", label: "Accounts", desc: "Customer profiles" },
        ].map((link) => (
          <Link key={link.href} href={link.href} className="panel rounded-2xl p-4 hover:shadow-md transition-shadow group">
            <p className="text-sm font-semibold text-slate-900 group-hover:text-orange-700">{link.label}</p>
            <p className="mt-0.5 text-xs text-slate-500">{link.desc}</p>
          </Link>
        ))}
      </section>
    </div>
  );
}
