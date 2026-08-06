"use client";

import React, { useMemo, useState } from "react";
import {
  BarChart,
  Bar,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  Cell,
} from "recharts";
import type { Payment } from "@/lib/types";

interface TransactionChartsProps {
  payments: Payment[];
  selectedAccountNumber: string | undefined;
}

type TimePeriod = "hour" | "day" | "month";

const STATUS_COLORS: Record<string, string> = {
  COMPLETED: "#10b981",
  SUCCESS: "#10b981",
  APPROVED: "#10b981",
  PENDING: "#f59e0b",
  PROCESSING: "#3b82f6",
  FAILED: "#ef4444",
  REJECTED: "#ef4444",
  CANCELLED: "#94a3b8",
  FLAGGED: "#8b5cf6",
};

function getStatusColor(status: string): string {
  return STATUS_COLORS[status.toUpperCase()] ?? "#64748b";
}

function groupKey(date: Date, period: TimePeriod): string {
  if (period === "hour") {
    return date.toLocaleDateString("en-IN", {
      month: "short",
      day: "numeric",
    }) + ` ${date.getHours().toString().padStart(2, "0")}:00`;
  }
  if (period === "month") {
    return date.toLocaleDateString("en-IN", { year: "numeric", month: "short" });
  }
  // day
  return date.toLocaleDateString("en-IN", {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

export function TransactionCharts({
  payments,
  selectedAccountNumber,
}: TransactionChartsProps) {
  const [period, setPeriod] = useState<TimePeriod>("day");

  const accountPayments = useMemo(
    () =>
      selectedAccountNumber
        ? payments.filter(
            (p) =>
              p.sourceAccount === selectedAccountNumber ||
              p.destinationAccount === selectedAccountNumber
          )
        : [],
    [payments, selectedAccountNumber]
  );

  // ── Amount Trend data ─────────────────────────────────────────────────────
  const amountTrendData = useMemo(() => {
    const buckets: Record<string, { label: string; sent: number; received: number; total: number }> = {};

    accountPayments.forEach((p) => {
      const key = groupKey(new Date(p.createdAt), period);
      if (!buckets[key]) buckets[key] = { label: key, sent: 0, received: 0, total: 0 };
      const amt = Number.parseFloat(p.amount) || 0;
      buckets[key].total += amt;
      if (p.sourceAccount === selectedAccountNumber) buckets[key].sent += amt;
      if (p.destinationAccount === selectedAccountNumber) buckets[key].received += amt;
    });

    return Object.values(buckets).sort(
      (a, b) => new Date(a.label).getTime() - new Date(b.label).getTime()
    );
  }, [accountPayments, period, selectedAccountNumber]);

  // ── Success Rate data ──────────────────────────────────────────────────────
  const successRateData = useMemo(() => {
    const buckets: Record<string, { status: string; count: number }> = {};
    accountPayments.forEach((p) => {
      const s = p.status ?? "UNKNOWN";
      if (!buckets[s]) buckets[s] = { status: s, count: 0 };
      buckets[s].count += 1;
    });
    return Object.values(buckets).sort((a, b) => b.count - a.count);
  }, [accountPayments]);

  if (!selectedAccountNumber) return null;

  const PERIOD_LABELS: Record<TimePeriod, string> = {
    hour: "Per Hour",
    day: "Per Day",
    month: "Per Month",
  };

  return (
    <section className="space-y-3">
      {/* Filter row */}
      <div className="flex items-center justify-between">
        <h3 className="text-base font-semibold text-slate-700">Transaction Analytics</h3>
        <div className="flex items-center gap-1 rounded-xl bg-slate-100 p-1">
          {(["hour", "day", "month"] as TimePeriod[]).map((p) => (
            <button
              key={p}
              onClick={() => setPeriod(p)}
              className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition-colors ${
                period === p
                  ? "bg-white text-orange-700 shadow-sm"
                  : "text-slate-500 hover:text-slate-800"
              }`}
            >
              {PERIOD_LABELS[p]}
            </button>
          ))}
        </div>
      </div>

      {/* Side-by-side charts */}
      <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
        {/* ── Chart 1: Amount over time ──────────────────────────── */}
        <div className="panel rounded-2xl p-6">
          <div className="mb-4 flex items-start justify-between">
            <div>
              <h4 className="text-sm font-semibold text-slate-900">
                Transaction Amounts
              </h4>
              <p className="text-xs text-slate-500 mt-0.5">
                Sent &amp; received totals — {PERIOD_LABELS[period].toLowerCase()}
              </p>
            </div>
          </div>
          {amountTrendData.length > 0 ? (
            <ResponsiveContainer width="100%" height={280}>
              <AreaChart data={amountTrendData} margin={{ left: 0, right: 8, top: 4, bottom: 0 }}>
                <defs>
                  <linearGradient id="sentGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#f97316" stopOpacity={0.25} />
                    <stop offset="95%" stopColor="#f97316" stopOpacity={0} />
                  </linearGradient>
                  <linearGradient id="recvGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.25} />
                    <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis
                  dataKey="label"
                  tick={{ fontSize: 11 }}
                  tickLine={false}
                  axisLine={false}
                  interval="preserveStartEnd"
                />
                <YAxis
                  tick={{ fontSize: 11 }}
                  tickLine={false}
                  axisLine={false}
                  tickFormatter={(v) =>
                    v >= 1_000_000
                      ? `₹${(v / 1_000_000).toFixed(1)}M`
                      : v >= 1_000
                      ? `₹${(v / 1_000).toFixed(0)}K`
                      : `₹${v}`
                  }
                />
                <Tooltip
                  contentStyle={{
                    backgroundColor: "#fff",
                    border: "1px solid #e2e8f0",
                    borderRadius: "10px",
                    fontSize: "12px",
                  }}
                  formatter={(value, name) => [
                    `₹${Number(value).toLocaleString("en-IN", { maximumFractionDigits: 2 })}`,
                    name === "sent" ? "Sent" : name === "received" ? "Received" : "Total",
                  ]}
                />
                <Legend formatter={(v) => v === "sent" ? "Sent" : v === "received" ? "Received" : "Total"} />
                <Area type="monotone" dataKey="sent" stroke="#f97316" strokeWidth={2} fill="url(#sentGrad)" dot={false} />
                <Area type="monotone" dataKey="received" stroke="#3b82f6" strokeWidth={2} fill="url(#recvGrad)" dot={false} />
              </AreaChart>
            </ResponsiveContainer>
          ) : (
            <div className="flex h-64 items-center justify-center text-sm text-slate-400">
              No transaction data for this period
            </div>
          )}
        </div>

        {/* ── Chart 2: Success rate by status ───────────────────── */}
        <div className="panel rounded-2xl p-6">
          <div className="mb-4">
            <h4 className="text-sm font-semibold text-slate-900">
              Transaction Success Rate
            </h4>
            <p className="text-xs text-slate-500 mt-0.5">
              Count of transactions grouped by status
            </p>
          </div>
          {successRateData.length > 0 ? (
            <ResponsiveContainer width="100%" height={280}>
              <BarChart
                data={successRateData}
                margin={{ left: 0, right: 8, top: 4, bottom: 0 }}
                barCategoryGap="30%"
              >
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" vertical={false} />
                <XAxis
                  dataKey="status"
                  tick={{ fontSize: 11 }}
                  tickLine={false}
                  axisLine={false}
                />
                <YAxis
                  tick={{ fontSize: 11 }}
                  tickLine={false}
                  axisLine={false}
                  allowDecimals={false}
                />
                <Tooltip
                  contentStyle={{
                    backgroundColor: "#fff",
                    border: "1px solid #e2e8f0",
                    borderRadius: "10px",
                    fontSize: "12px",
                  }}
                  formatter={(value) => [`${value} transaction(s)`, "Count"]}
                />
                <Bar dataKey="count" radius={[6, 6, 0, 0]} maxBarSize={60}>
                  {successRateData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={getStatusColor(entry.status)} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <div className="flex h-64 items-center justify-center text-sm text-slate-400">
              No transaction data available
            </div>
          )}

          {/* Legend */}
          {successRateData.length > 0 && (
            <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1.5">
              {successRateData.map((entry) => (
                <span key={entry.status} className="flex items-center gap-1.5 text-xs text-slate-600">
                  <span
                    className="inline-block h-2.5 w-2.5 rounded-full"
                    style={{ backgroundColor: getStatusColor(entry.status) }}
                  />
                  {entry.status}
                  <span className="font-semibold text-slate-800">{entry.count}</span>
                </span>
              ))}
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
