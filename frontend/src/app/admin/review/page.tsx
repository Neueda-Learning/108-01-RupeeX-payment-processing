"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { getPendingAdminApprovalPayments, adminApprovePayment, adminDeclinePayment } from "@/lib/api";
import type { Payment } from "@/lib/types";
import { formatCurrency, formatDateTime } from "@/lib/format";
import { useUserStore } from "@/lib/user-store";

export default function AdminReviewPage() {
  const [payments, setPayments] = useState<Payment[]>([]);
  const [loading, setLoading] = useState(true);
  const [processing, setProcessing] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [declineReason, setDeclineReason] = useState<string>("");
  const [showDeclineModal, setShowDeclineModal] = useState<number | null>(null);
  const { currentUser } = useUserStore();
  const router = useRouter();

  // Only admins can access this page
  useEffect(() => {
    if (currentUser && currentUser.role !== "admin") {
      router.replace("/payments");
    }
  }, [currentUser, router]);

  const loadPendingPayments = () => {
    setLoading(true);
    setError(null);
    getPendingAdminApprovalPayments()
      .then((rows) => {
        setPayments(rows);
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : "Failed to load payments");
      })
      .finally(() => {
        setLoading(false);
      });
  };

  // Fetch pending approval payments
  useEffect(() => {
    loadPendingPayments();
  }, []);

  const handleApprove = async (paymentId: number) => {
    setProcessing(paymentId);
    setError(null);
    try {
      await adminApprovePayment(paymentId);
      // Remove from list after approval
      setPayments((prev) => prev.filter((p) => p.id !== paymentId));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to approve payment");
    } finally {
      setProcessing(null);
    }
  };

  const handleDecline = async (paymentId: number) => {
    setProcessing(paymentId);
    setError(null);
    try {
      await adminDeclinePayment(paymentId, declineReason || "Declined by administrator");
      // Remove from list after decline
      setPayments((prev) => prev.filter((p) => p.id !== paymentId));
      setShowDeclineModal(null);
      setDeclineReason("");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to decline payment");
    } finally {
      setProcessing(null);
    }
  };

  return (
    <div className="mx-auto max-w-7xl space-y-8 px-6 py-10 lg:px-8">
      <header className="space-y-2">
        <p className="text-sm font-semibold uppercase tracking-wide text-orange-700">
          Admin Review
        </p>
        <h1 className="text-3xl font-bold tracking-tight text-slate-900">
          Payments Requiring Approval
        </h1>
        <p className="text-slate-600">
          Review and approve/decline payments with risk scores between 80-100 that require manual admin approval.
        </p>
      </header>

      {error && (
        <div className="rounded-lg border border-red-200 bg-red-50 p-4">
          <p className="text-sm text-red-800">{error}</p>
        </div>
      )}

      <section className="panel rounded-2xl overflow-hidden">
        <div className="border-b border-black/5 px-6 py-4 flex items-center justify-between">
          <div>
            <h2 className="text-lg font-semibold text-slate-900">Pending Approval Queue</h2>
            <p className="text-sm text-slate-500">{payments.length} payment(s) awaiting review</p>
          </div>
          <button
            onClick={loadPendingPayments}
            className="rounded-lg bg-slate-100 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-200"
          >
            Refresh
          </button>
        </div>

        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-black/5">
            <thead className="bg-slate-50">
              <tr>
                {[
                  "Payment Ref",
                  "Route",
                  "Amount",
                  "Risk Score",
                  "Risk Category",
                  "Created",
                  "Actions",
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
                    No payments requiring approval at this time.
                  </td>
                </tr>
              ) : (
                payments.map((payment) => (
                  <tr key={payment.id} className="hover:bg-slate-50">
                    <td className="whitespace-nowrap px-6 py-4 text-sm">
                      <Link
                        href={`/payments/${payment.id}`}
                        className="font-medium text-orange-700 hover:text-orange-800"
                      >
                        {payment.reference ?? `Payment #${payment.id}`}
                      </Link>
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
                      {payment.riskScore && (
                        <span
                          className={`inline-flex rounded-full px-3 py-1 text-xs font-bold ${
                            payment.riskScore.score >= 95
                              ? "bg-red-100 text-red-800"
                              : "bg-orange-100 text-orange-800"
                          }`}
                        >
                          {payment.riskScore.score}
                        </span>
                      )}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm">
                      {payment.riskScore && (
                        <div>
                          <p className="font-medium text-slate-900">
                            {payment.riskScore.category}
                          </p>
                          <p className="text-xs text-slate-500">
                            {payment.riskScore.decision}
                          </p>
                        </div>
                      )}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-500">
                      {formatDateTime(payment.createdAt)}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm">
                      <div className="flex items-center space-x-2">
                        <button
                          onClick={() => handleApprove(payment.id)}
                          disabled={processing === payment.id}
                          className="rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-emerald-700 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                          {processing === payment.id ? "..." : "Approve"}
                        </button>
                        <button
                          onClick={() => setShowDeclineModal(payment.id)}
                          disabled={processing === payment.id}
                          className="rounded-lg bg-red-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                          Decline
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>

      {/* Decline Modal */}
      {showDeclineModal !== null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="mx-4 w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl">
            <h3 className="text-lg font-semibold text-slate-900">
              Decline Payment
            </h3>
            <p className="mt-2 text-sm text-slate-600">
              Please provide a reason for declining this payment.
            </p>
            <textarea
              value={declineReason}
              onChange={(e) => setDeclineReason(e.target.value)}
              placeholder="Enter decline reason..."
              className="mt-4 w-full rounded-lg border border-slate-200 p-3 text-sm focus:border-orange-500 focus:outline-none focus:ring-2 focus:ring-orange-500/20"
              rows={4}
            />
            <div className="mt-6 flex justify-end space-x-3">
              <button
                onClick={() => {
                  setShowDeclineModal(null);
                  setDeclineReason("");
                }}
                className="rounded-lg bg-slate-100 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-200"
              >
                Cancel
              </button>
              <button
                onClick={() => handleDecline(showDeclineModal)}
                disabled={processing !== null}
                className="rounded-lg bg-red-600 px-4 py-2 text-sm font-semibold text-white hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {processing === showDeclineModal ? "Declining..." : "Decline Payment"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

