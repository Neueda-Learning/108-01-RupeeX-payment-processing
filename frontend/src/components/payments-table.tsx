import type { Payment } from "@/lib/types";
import { formatCurrency, formatDateTime } from "@/lib/format";
import { StatusBadge } from "./status-badge";

export function PaymentsTable({ payments }: { payments: Payment[] }) {
  return (
    <div className="overflow-hidden rounded-2xl border border-black/5 bg-white shadow-sm dark:border-white/10 dark:bg-slate-900">
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-black/5 dark:divide-white/10">
          <thead className="bg-slate-50 dark:bg-slate-800/50">
            <tr>
              {["Reference", "Route", "Amount", "Status", "Updated"].map((col) => (
                <th
                  key={col}
                  scope="col"
                  className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400"
                >
                  {col}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-black/5 dark:divide-white/10">
            {payments.map((payment) => (
              <tr
                key={payment.id}
                className="transition hover:bg-slate-50 dark:hover:bg-slate-800/40"
              >
                <td className="whitespace-nowrap px-6 py-4">
                  <div className="text-sm font-medium text-slate-900 dark:text-white">
                    {payment.reference ?? `Payment #${payment.id}`}
                  </div>
                  <div className="text-xs text-slate-500 dark:text-slate-400">
                    {payment.idempotencyKey}
                  </div>
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-600 dark:text-slate-300">
                  {payment.sourceAccount}
                  <span className="mx-1.5 text-slate-400">→</span>
                  {payment.destinationAccount}
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-sm font-semibold text-slate-900 dark:text-white">
                  {formatCurrency(payment.amount, payment.currency)}
                </td>
                <td className="whitespace-nowrap px-6 py-4">
                  <StatusBadge status={payment.status} />
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-500 dark:text-slate-400">
                  {formatDateTime(payment.updatedAt)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
