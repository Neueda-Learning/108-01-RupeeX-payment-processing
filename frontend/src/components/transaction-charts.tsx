"use client";

import React from "react";
import {
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from "recharts";
import type { Payment } from "@/lib/types";

interface TransactionChartsProps {
  payments: Payment[];
  selectedAccountNumber: string | undefined;
}

export function TransactionCharts({
  payments,
  selectedAccountNumber,
}: TransactionChartsProps) {
  if (!selectedAccountNumber) return null;

  // Prepare data for sent vs received over time
  const timelineData: Record<string, { date: string; sent: number; received: number }> = {};

  payments.forEach((p) => {
    const date = new Date(p.createdAt).toLocaleDateString("en-IN", {
      year: "numeric",
      month: "short",
      day: "numeric",
    });

    if (!timelineData[date]) {
      timelineData[date] = { date, sent: 0, received: 0 };
    }

    if (p.sourceAccount === selectedAccountNumber) {
      timelineData[date].sent += Number.parseFloat(p.amount) || 0;
    }
    if (p.destinationAccount === selectedAccountNumber) {
      timelineData[date].received += Number.parseFloat(p.amount) || 0;
    }
  });

  const timelineChartData = Object.values(timelineData).sort(
    (a, b) => new Date(a.date).getTime() - new Date(b.date).getTime()
  );

  // Prepare data for status distribution
  const statusData: Record<string, number> = {};
  payments
    .filter(
      (p) =>
        p.sourceAccount === selectedAccountNumber ||
        p.destinationAccount === selectedAccountNumber
    )
    .forEach((p) => {
      statusData[p.status] = (statusData[p.status] || 0) + 1;
    });

  const statusChartData = Object.entries(statusData).map(([status, count]) => ({
    name: status,
    value: count,
  }));

  const COLORS = [
    "#f97316",
    "#3b82f6",
    "#10b981",
    "#ef4444",
    "#8b5cf6",
    "#ec4899",
    "#f59e0b",
    "#06b6d4",
  ];

  return (
    <section className="space-y-6">
      {/* Amount Trend Chart */}
      <div className="panel rounded-2xl p-6">
        <h3 className="text-lg font-semibold text-slate-900 mb-4">
          Amount Trend
        </h3>
        {timelineChartData.length > 0 ? (
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={timelineChartData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis dataKey="date" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} />
              <Tooltip
                contentStyle={{
                  backgroundColor: "#fff",
                  border: "1px solid #e2e8f0",
                  borderRadius: "8px",
                }}
                formatter={(value) =>
                  `₹${Number(value).toLocaleString("en-IN", {
                    maximumFractionDigits: 2,
                  })}`
                }
              />
              <Legend />
              <Bar dataKey="sent" fill="#f97316" radius={[8, 8, 0, 0]} />
              <Bar dataKey="received" fill="#3b82f6" radius={[8, 8, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        ) : (
          <p className="text-center text-sm text-slate-400">
            No transaction data available
          </p>
        )}
      </div>

      {/* Status Distribution Chart */}
      <div className="panel rounded-2xl p-6">
        <h3 className="text-lg font-semibold text-slate-900 mb-4">
          Status Distribution
        </h3>
        {statusChartData.length > 0 ? (
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={statusChartData}
                cx="50%"
                cy="50%"
                labelLine={false}
                label={({ name, value }) => `${name} (${value})`}
                outerRadius={100}
                fill="#8884d8"
                dataKey="value"
              >
                {statusChartData.map((entry, index) => (
                  <Cell
                    key={`cell-${index}`}
                    fill={COLORS[index % COLORS.length]}
                  />
                ))}
              </Pie>
              <Tooltip formatter={(value) => `${value} transaction(s)`} />
            </PieChart>
          </ResponsiveContainer>
        ) : (
          <p className="text-center text-sm text-slate-400">
            No transaction data available
          </p>
        )}
      </div>
    </section>
  );
}

