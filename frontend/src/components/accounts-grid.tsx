import type { Account } from "@/lib/types";
import { StatusBadge } from "./status-badge";
import { Landmark } from "lucide-react";

export function AccountsGrid({ accounts }: { accounts: Account[] }) {
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
      {accounts.map((account) => (
        <div
          key={account.id}
          className="rounded-2xl border border-black/5 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900"
        >
          <div className="flex items-start justify-between">
            <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-slate-100 text-slate-500 dark:bg-slate-800 dark:text-slate-400">
              <Landmark className="h-4.5 w-4.5" />
            </span>
            <StatusBadge status={account.status} />
          </div>
          <p className="mt-4 truncate text-sm font-semibold text-slate-900 dark:text-white">
            {account.accountHolder}
          </p>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            {account.accountNumber}
          </p>
          <div className="mt-3 flex items-center justify-between text-xs text-slate-500 dark:text-slate-400">
            <span>{account.bankName ?? "—"}</span>
            <span className="rounded-full bg-slate-100 px-2 py-0.5 font-medium text-slate-600 dark:bg-slate-800 dark:text-slate-300">
              {account.currency}
            </span>
          </div>
        </div>
      ))}
    </div>
  );
}
