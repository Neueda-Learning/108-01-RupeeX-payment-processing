"use client";

import { useState } from "react";
import type { CreatePaymentInput, Payment } from "@/lib/types";
import { createPayment } from "@/lib/api";

const DEFAULT_FORM: CreatePaymentInput = {
  amount: 1000,
  currency: "INR",
  sourceAccount: "ACC-10293",
  destinationAccount: "ACC-58213",
  originCountry: "IN",
  destinationCountry: "IN",
};

export function PaymentCreateForm({
  onCreated,
}: {
  onCreated: (payment: Payment) => void;
}) {
  const [form, setForm] = useState<CreatePaymentInput>(DEFAULT_FORM);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleChange =
    (field: keyof CreatePaymentInput) =>
    (event: React.ChangeEvent<HTMLInputElement>) => {
      const value = event.target.value;
      setForm((current) => ({
        ...current,
        [field]: field === "amount" ? Number(value) : value,
      }));
    };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);

    try {
      const created = await createPayment(form);
      onCreated(created);
      setForm(DEFAULT_FORM);
    } catch (submissionError) {
      setError(
        submissionError instanceof Error
          ? submissionError.message
          : "Unable to create payment",
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="rounded-2xl border border-black/5 bg-white p-6 shadow-sm dark:border-white/10 dark:bg-slate-900"
    >
      <h2 className="text-lg font-semibold text-slate-900 dark:text-white">
        Create payment
      </h2>
      <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
        Submit a payment to start validation, risk checks, queueing, and settlement.
      </p>

      <div className="mt-5 grid grid-cols-1 gap-4 sm:grid-cols-2">
        <label className="text-sm">
          <span className="mb-1 block font-medium text-slate-700 dark:text-slate-300">
            Amount
          </span>
          <input
            type="number"
            min="0.01"
            step="0.01"
            value={form.amount}
            onChange={handleChange("amount")}
            required
            className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-slate-900 outline-none ring-emerald-500/30 focus:ring dark:border-slate-700 dark:bg-slate-950 dark:text-white"
          />
        </label>

        <label className="text-sm">
          <span className="mb-1 block font-medium text-slate-700 dark:text-slate-300">
            Currency
          </span>
          <input
            value={form.currency}
            onChange={handleChange("currency")}
            required
            className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-slate-900 outline-none ring-emerald-500/30 focus:ring dark:border-slate-700 dark:bg-slate-950 dark:text-white"
          />
        </label>

        <label className="text-sm">
          <span className="mb-1 block font-medium text-slate-700 dark:text-slate-300">
            Source account
          </span>
          <input
            value={form.sourceAccount}
            onChange={handleChange("sourceAccount")}
            required
            className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-slate-900 outline-none ring-emerald-500/30 focus:ring dark:border-slate-700 dark:bg-slate-950 dark:text-white"
          />
        </label>

        <label className="text-sm">
          <span className="mb-1 block font-medium text-slate-700 dark:text-slate-300">
            Destination account
          </span>
          <input
            value={form.destinationAccount}
            onChange={handleChange("destinationAccount")}
            required
            className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-slate-900 outline-none ring-emerald-500/30 focus:ring dark:border-slate-700 dark:bg-slate-950 dark:text-white"
          />
        </label>

        <label className="text-sm">
          <span className="mb-1 block font-medium text-slate-700 dark:text-slate-300">
            Origin country
          </span>
          <input
            value={form.originCountry}
            onChange={handleChange("originCountry")}
            required
            className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-slate-900 outline-none ring-emerald-500/30 focus:ring dark:border-slate-700 dark:bg-slate-950 dark:text-white"
          />
        </label>

        <label className="text-sm">
          <span className="mb-1 block font-medium text-slate-700 dark:text-slate-300">
            Destination country
          </span>
          <input
            value={form.destinationCountry}
            onChange={handleChange("destinationCountry")}
            required
            className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-slate-900 outline-none ring-emerald-500/30 focus:ring dark:border-slate-700 dark:bg-slate-950 dark:text-white"
          />
        </label>
      </div>

      {error && (
        <p className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-500/10 dark:text-red-300">
          {error}
        </p>
      )}

      <button
        type="submit"
        disabled={isSubmitting}
        className="mt-5 inline-flex items-center justify-center rounded-lg bg-emerald-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-60"
      >
        {isSubmitting ? "Submitting..." : "Create payment"}
      </button>
    </form>
  );
}
