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
    let cancelled = false;
    getPayments()
      .then((rows) => {
        if (!cancelled) {
          setPayments(rows);
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

  return (
    <div className="mx-auto max-w-7xl space-y-8 px-6 py-10 lg:px-8">
      <header className="space-y-2">
        <p className="text-sm font-semibold uppercase tracking-wide text-orange-700">
          Payments Workspace
        </p>
        <h1 className="text-3xl font-bold tracking-tight text-slate-900">
          Create a payment and track lifecycle status
        </h1>
        <p className="text-slate-600">
          Use this screen to submit transactions and monitor status updates for
          each payment.
        </p>
      </header>

      <PaymentCreateForm
        onCreated={(payment) => setPayments((current) => [payment, ...current])}
      />

      <section className="panel rounded-2xl overflow-hidden">
        <div className="border-b border-black/5 px-6 py-4">
          <h2 className="text-lg font-semibold text-slate-900">Payment list</h2>
        </div>

        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-black/5">
            <thead className="bg-slate-50">
              <tr>
                {[
                  "Payment Label",
                  "Source -> Destination",
                  "Amount",
                  "Risk Score",
                  "Current Status",
                  "Created",
                  "Open Details",
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
                    <td className="whitespace-nowrap px-6 py-4 text-sm">
                      <p className="font-medium text-slate-900">
                        {payment.reference ?? `Payment #${payment.id}`}
                      </p>
                      <p className="text-xs text-slate-500">
                        {payment.idempotencyKey}
                      </p>
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-600">
                      {payment.sourceAccount}
                      <span className="mx-1 text-slate-400">→</span>
                      {payment.destinationAccount}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm font-semibold text-slate-900">
                      {formatCurrency(payment.amount, payment.currency)}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">
                      {payment.riskScore ? (
                        <div className="flex flex-col gap-1">
                          <span
                            className={`inline-flex w-fit rounded-full px-2 py-0.5 text-xs font-bold ${
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
                              ? "⚠️ Manual"
                              : "✓ Auto"}
                          </span>
                        </div>
                      ) : (
                        <span className="text-xs text-slate-400">—</span>
                      )}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">
                      <StatusBadge status={payment.status} />
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-500">
                      {formatDateTime(payment.createdAt)}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm">
                      <Link
                        href={`/payments/${payment.id}`}
                        className="font-medium text-orange-700 hover:text-orange-800"
                      >
                        Details
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
