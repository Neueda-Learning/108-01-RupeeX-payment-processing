"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getPayments } from "@/lib/api";
import type { Payment } from "@/lib/types";
import { formatCurrency, formatDateTime } from "@/lib/format";
import { StatusBadge } from "@/components/status-badge";
import { PaymentCreateForm } from "@/components/payment-create-form";
import { useUserStore } from "@/lib/user-store";

export default function PaymentsPage() {
  const [payments, setPayments] = useState<Payment[]>([]);
  const [loading, setLoading] = useState(true);
  const { currentUser } = useUserStore();
  const router = useRouter();

  // Members are not allowed on this page — redirect to their account view
  useEffect(() => {
    if (currentUser?.role === "member") {
      router.replace("/accounts");
    }
  }, [currentUser, router]);

  // Fetch payments
  useEffect(() => {
    let cancelled = false;
    getPayments()
      .then((rows) => {
        if (!cancelled) {
          // Sort payments by createdAt descending (newest first)
          const sorted = [...rows].sort((a, b) => {
            return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
          });
          console.log('Fetched payments:', sorted.length, 'First payment:', sorted[0]?.id, sorted[0]?.createdAt);
          console.log('Risk scores:', sorted.map(p => ({ id: p.id, riskScore: p.riskScore?.score })));
          setPayments(sorted);
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

  const handlePaymentCreated = (payment: Payment) => {
    setPayments((current) => {
      // Add new payment at the top and re-sort to maintain order
      const updated = [payment, ...current];
      return updated.sort((a, b) => {
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
      });
    });
  };

  return (
    <div className="mx-auto max-w-7xl space-y-6 px-4 py-6 lg:px-6">
      <header className="space-y-1">
        <p className="text-xs font-semibold uppercase tracking-wide text-orange-700">
          Payments Workspace
        </p>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">
          Create and track payments
        </h1>
        <p className="text-xs text-slate-600">
          Submit transactions and monitor status updates
        </p>
      </header>

      <PaymentCreateForm
        onCreated={handlePaymentCreated}
      />

      <section className="panel rounded-2xl overflow-hidden">
        <div className="border-b border-black/5 px-4 py-3">
          <h2 className="text-base font-semibold text-slate-900">Payment list</h2>
        </div>

        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-black/5">
            <thead className="bg-slate-50">
              <tr>
                {[
                  "Payment",
                  "Route",
                  "Amount",
                  "Risk",
                  "Status",
                  "Created",
                  "Details",
                ].map((header) => (
                  <th
                    key={header}
                    className="px-3 py-2 text-left text-xs font-semibold uppercase tracking-wider text-slate-500"
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
                    className="px-3 py-6 text-center text-xs text-slate-500"
                  >
                    Loading payments...
                  </td>
                </tr>
              ) : payments.length === 0 ? (
                <tr>
                  <td
                    colSpan={7}
                    className="px-3 py-6 text-center text-xs text-slate-500"
                  >
                    No payments found.
                  </td>
                </tr>
              ) : (
                payments.map((payment) => (
                  <tr key={payment.id}>
                    <td className="px-3 py-2 text-xs">
                      <p className="font-medium text-slate-900 truncate">
                        {payment.reference ?? `Pmt #${payment.id}`}
                      </p>
                      <p className="text-xs text-slate-500 truncate">
                        {payment.idempotencyKey?.substring(0, 12)}...
                      </p>
                    </td>
                    <td className="px-3 py-2 text-xs text-slate-600">
                      <div className="flex items-center gap-0.5 truncate">
                        <span className="truncate">{payment.sourceAccount?.substring(0, 8)}</span>
                        <span className="text-slate-400 flex-shrink-0">→</span>
                        <span className="truncate">{payment.destinationAccount?.substring(0, 8)}</span>
                      </div>
                    </td>
                    <td className="px-3 py-2 text-xs font-semibold text-slate-900 whitespace-nowrap">
                      {formatCurrency(payment.amount, payment.currency)}
                    </td>
                    <td className="px-3 py-2">
                      {payment.riskScore ? (
                        <div className="flex flex-col gap-0.5">
                          <span
                            className={`inline-flex w-fit rounded-full px-1.5 py-0.5 text-xs font-bold ${
                              payment.riskScore.score >= 81
                                ? "bg-red-100 text-red-700"
                                : payment.riskScore.score >= 50
                                  ? "bg-orange-100 text-orange-700"
                                  : "bg-emerald-100 text-emerald-700"
                            }`}
                          >
                            {payment.riskScore.score}
                          </span>
                          <span className="text-xs text-slate-500">
                            {payment.riskScore.decision === "Manual Review"
                              ? "⚠️"
                              : "✓"}
                          </span>
                        </div>
                      ) : (
                        <span className="text-xs text-slate-400">—</span>
                      )}
                    </td>
                    <td className="px-3 py-2">
                      <StatusBadge status={payment.status} />
                    </td>
                    <td className="px-3 py-2 text-xs text-slate-500 whitespace-nowrap">
                      {formatDateTime(payment.createdAt).split(" ")[0]}
                    </td>
                    <td className="px-3 py-2 text-xs">
                      <Link
                        href={`/payments/${payment.id}`}
                        className="font-medium text-orange-700 hover:text-orange-800 hover:underline"
                      >
                        View
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
