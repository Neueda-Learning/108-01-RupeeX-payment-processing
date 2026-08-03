"use client";

import { useEffect, useState } from "react";
import { cancelPayment, getDeadLetterQueue, retryPayment } from "@/lib/api";
import type { DeadLetterEntry } from "@/lib/types";
import { formatDateTime } from "@/lib/format";

export default function DeadLetterQueuePage() {
  const [entries, setEntries] = useState<DeadLetterEntry[]>([]);
  const [error, setError] = useState<string | null>(null);

  const loadEntries = async () => {
    try {
      setEntries(await getDeadLetterQueue());
      setError(null);
    } catch (loadError) {
      setError(
        loadError instanceof Error ? loadError.message : "Unable to load DLQ",
      );
    }
  };

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void loadEntries();
    }, 0);
    return () => window.clearTimeout(timer);
  }, []);

  return (
    <div className="mx-auto max-w-6xl space-y-8 px-6 py-10 lg:px-8">
      <header>
        <p className="text-sm font-semibold uppercase tracking-wide text-orange-700">
          Dead Letter Queue
        </p>
        <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-900">
          Failed payment recovery
        </h1>
        <p className="text-slate-600">
          Inspect payments that exceeded retry limits and trigger retry or
          cancel actions.
        </p>
      </header>

      {error && (
        <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </p>
      )}

      <section className="panel rounded-2xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-black/5">
            <thead className="bg-slate-50">
              <tr>
                {["Payment ID", "Reason", "Retries", "Created", "Actions"].map(
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
              {entries.length === 0 ? (
                <tr>
                  <td
                    colSpan={5}
                    className="px-6 py-8 text-center text-sm text-slate-500"
                  >
                    DLQ endpoint returned no records. If backend DLQ API is not
                    enabled yet, this table will remain empty.
                  </td>
                </tr>
              ) : (
                entries.map((entry) => (
                  <tr key={entry.id}>
                    <td className="px-6 py-4 text-sm font-medium text-slate-900">
                      #{entry.paymentId}
                    </td>
                    <td className="px-6 py-4 text-sm text-slate-600">
                      {entry.reason}
                    </td>
                    <td className="px-6 py-4 text-sm">
                      {entry.lastRetryCount}
                    </td>
                    <td className="px-6 py-4 text-sm text-slate-500">
                      {entry.createdAt ? formatDateTime(entry.createdAt) : "-"}
                    </td>
                    <td className="px-6 py-4 text-sm">
                      <div className="flex gap-2">
                        <button
                          onClick={async () => {
                            await retryPayment(entry.paymentId);
                            await loadEntries();
                          }}
                          className="rounded-md border border-slate-200 px-3 py-1 font-medium text-slate-700"
                        >
                          Retry
                        </button>
                        <button
                          onClick={async () => {
                            await cancelPayment(entry.paymentId);
                            await loadEntries();
                          }}
                          className="rounded-md bg-red-600 px-3 py-1 font-medium text-white"
                        >
                          Cancel
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
    </div>
  );
}
