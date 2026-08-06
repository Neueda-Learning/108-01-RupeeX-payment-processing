"use client";

import { useEffect, useState } from "react";
import type { Account, CreatePaymentInput, ExchangeRateResult, Payment } from "@/lib/types";
import { convertCurrency, createPayment, getAccounts, sendOtp, verifyOtp } from "@/lib/api";

// Countries are now auto-fetched from source and destination accounts,
// so COUNTRIES list is no longer needed

const CURRENCIES = [
  "INR",
  "USD",
  "EUR",
  "GBP",
  "SGD",
  "AED",
  "JPY",
  "CNY",
  "CAD",
  "AUD",
];

export function PaymentCreateForm({
  onCreated,
  defaultSourceAccount,
}: {
  onCreated: (payment: Payment) => void;
  defaultSourceAccount?: string;
}) {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [form, setForm] = useState<CreatePaymentInput>({
    amount: 1000,
    currency: "INR",
    sourceAccount: defaultSourceAccount ?? "",
    destinationAccount: "",
    originCountry: "IN",
    destinationCountry: "IN",
    payerEmail: "",
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Scheduled payment state
  const [isScheduled, setIsScheduled] = useState(false);
  const [scheduledAt, setScheduledAt] = useState("");

  // Live currency conversion preview state
  const [exchange, setExchange] = useState<ExchangeRateResult | null>(null);
  const [exchangeLoading, setExchangeLoading] = useState(false);
  const [exchangeError, setExchangeError] = useState<string | null>(null);

  // OTP flow state
  const [step, setStep] = useState<"form" | "otp">("form");
  const [otpValue, setOtpValue] = useState("");
  const [otpError, setOtpError] = useState<string | null>(null);
  const [isVerifying, setIsVerifying] = useState(false);
  const [maskedEmail, setMaskedEmail] = useState("");

  useEffect(() => {
    getAccounts()
      .then((accs) => {
        setAccounts(accs);
        if (!defaultSourceAccount && accs.length > 0) {
          setForm((f) => ({
            ...f,
            sourceAccount: accs[0].accountNumber,
            payerEmail: accs[0].email ?? "",
          }));
        }
      })
      .catch(() => {
        /* non-critical */
      });
  }, [defaultSourceAccount]);

  // Fetch a live conversion preview whenever the amount, payment currency, or
  // destination account (whose native currency may differ) changes.
  useEffect(() => {
    let cancelled = false;
    const destAcc = accounts.find(
      (a) => a.accountNumber === form.destinationAccount,
    );
    const targetCurrency = destAcc?.currency;

    // Early return if conditions aren't met - skip the async fetch
    if (!targetCurrency || targetCurrency === form.currency || !form.amount) {
      // Nothing to convert for this combination; the preview below is hidden
      // via the derived `showExchangePreview` check, so no state reset needed.
      return;
    }

    const timer = setTimeout(() => {
      // State updates live inside this callback (fired by the debounce timer),
      // not synchronously in the effect body, so they don't cause cascading renders.
      setExchangeLoading(true);
      setExchangeError(null);
      convertCurrency(form.amount, form.currency, targetCurrency)
        .then((result) => {
          if (!cancelled) setExchange(result);
        })
        .catch((err) => {
          if (!cancelled) {
            setExchange(null);
            setExchangeError(
              err instanceof Error ? err.message : "Exchange rate unavailable",
            );
          }
        })
        .finally(() => {
          if (!cancelled) setExchangeLoading(false);
        });
    }, 400); // debounce rapid amount typing
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [form.amount, form.currency, form.destinationAccount, accounts]);

  // Auto-set origin country AND payer email from selected source account
  const handleSourceAccountChange = (accountNumber: string) => {
    const src = accounts.find((a) => a.accountNumber === accountNumber);
    setForm((f) => ({
      ...f,
      sourceAccount: accountNumber,
      originCountry: src?.countryCode ?? f.originCountry,
      payerEmail: src?.email ?? "",
    }));
  };

  // Auto-set destination country from selected destination account
  const handleDestinationAccountChange = (accountNumber: string) => {
    const dest = accounts.find((a) => a.accountNumber === accountNumber);
    setForm((f) => ({
      ...f,
      destinationAccount: accountNumber,
      destinationCountry: dest?.countryCode ?? f.destinationCountry,
    }));
  };

  const set =
    (field: keyof CreatePaymentInput) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
      setForm((f) => ({
        ...f,
        [field]: field === "amount" ? Number(e.target.value) : e.target.value,
      }));

  /** Masks an email for display: "payer@example.com" → "pa***@example.com" */
  const maskEmail = (email: string): string => {
    const [local, domain] = email.split("@");
    if (!domain) return email;
    const visible = local.slice(0, Math.min(2, local.length));
    return `${visible}***@${domain}`;
  };

  // Step 1 – submit form: send OTP to the email registered on the source account
  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!form.payerEmail) {
      setError(
        "No email address is registered on this account. Please update the account with a valid email before proceeding."
      );
      return;
    }
    if (isScheduled) {
      if (!scheduledAt) {
        setError("Please choose a date and time to schedule this payment.");
        return;
      }
      if (new Date(scheduledAt).getTime() <= Date.now()) {
        setError("Scheduled time must be in the future (IST).");
        return;
      }
    }
    setError(null);
    setIsSubmitting(true);
    try {
      await sendOtp(form.payerEmail, form.sourceAccount);
      setMaskedEmail(maskEmail(form.payerEmail));
      setStep("otp");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to send OTP");
    } finally {
      setIsSubmitting(false);
    }
  };

  // Step 2 – verify OTP, then create payment
  const handleOtpVerify = async () => {
    if (otpValue.length !== 4) {
      setOtpError("Please enter the 4-digit OTP.");
      return;
    }
    setOtpError(null);
    setIsVerifying(true);
    try {
      const result = await verifyOtp(form.payerEmail!, otpValue);
      if (!result.valid) {
        setOtpError(result.message ?? "Invalid or expired OTP. Try again.");
        return;
      }
      const created = await createPayment({
        ...form,
        ...(isScheduled && scheduledAt ? { scheduledAt: `${scheduledAt}:00` } : {}),
      });
      onCreated(created);
      // Reset form
      setForm((f) => ({ ...f, amount: 1000, destinationAccount: "", payerEmail: "" }));
      setIsScheduled(false);
      setScheduledAt("");
      setStep("form");
      setOtpValue("");
      setMaskedEmail("");
    } catch (err) {
      setOtpError(err instanceof Error ? err.message : "Unable to complete payment");
    } finally {
      setIsVerifying(false);
    }
  };

  const sourceAcc = accounts.find(
    (a) => a.accountNumber === form.sourceAccount,
  );
  const destOptions = accounts.filter(
    (a) => a.accountNumber !== form.sourceAccount,
  );
  const destAcc = accounts.find(
    (a) => a.accountNumber === form.destinationAccount,
  );
  const showExchangePreview = Boolean(
    destAcc?.currency && destAcc.currency !== form.currency && form.amount,
  );

  // ── OTP verification step ──────────────────────────────────────────────────
  if (step === "otp") {
    return (
      <div className="panel rounded-2xl p-6">
        <h2 className="text-lg font-semibold text-slate-900">Verify your identity</h2>
        <p className="mt-1 text-sm text-slate-500">
          A 4-digit OTP has been sent to{" "}
          <span className="font-medium text-slate-700">{maskedEmail}</span>.
          Enter it below to authorise the payment.
        </p>

        <div className="mt-5">
          <label className="text-sm">
            <span className="mb-1 block font-medium text-slate-700">One-time password</span>
            <input
              type="text"
              inputMode="numeric"
              maxLength={4}
              pattern="\d{4}"
              placeholder="_ _ _ _"
              value={otpValue}
              onChange={(e) => {
                const val = e.target.value.replace(/\D/g, "").slice(0, 4);
                setOtpValue(val);
                setOtpError(null);
              }}
              className="w-40 rounded-lg border border-slate-200 bg-white px-3 py-2 text-center text-2xl tracking-widest text-slate-900 outline-none ring-orange-500/30 focus:ring"
            />
          </label>
        </div>

        {otpError && (
          <p className="mt-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
            {otpError}
          </p>
        )}

        <div className="mt-5 flex items-center gap-3">
          <button
            type="button"
            onClick={handleOtpVerify}
            disabled={isVerifying || otpValue.length !== 4}
            className="inline-flex items-center justify-center rounded-lg bg-orange-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-orange-700 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isVerifying ? "Verifying…" : "Verify & Pay"}
          </button>
          <button
            type="button"
            onClick={() => { setStep("form"); setOtpValue(""); setOtpError(null); }}
            className="text-sm text-slate-500 hover:text-slate-700"
          >
            Cancel
          </button>
        </div>
      </div>
    );
  }

  // ── Payment form ───────────────────────────────────────────────────────────
  return (
    <form onSubmit={handleSubmit} className="panel rounded-2xl p-6">
      <h2 className="text-lg font-semibold text-slate-900">Create payment</h2>
      <p className="mt-1 text-sm text-slate-500">
        Submit a payment to start validation, risk checks, queueing, and
        settlement.
      </p>

      <div className="mt-5 grid grid-cols-1 gap-4 sm:grid-cols-2">
        {/* Source account */}
        <label className="text-sm sm:col-span-2">
          <span className="mb-1 block font-medium text-slate-700">
            Source account
          </span>
          <select
            value={form.sourceAccount}
            onChange={(e) => handleSourceAccountChange(e.target.value)}
            required
            className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-slate-900 outline-none ring-orange-500/30 focus:ring"
          >
            <option value="">Select source account…</option>
            {accounts.map((a) => (
              <option key={a.id} value={a.accountNumber}>
                {a.accountNumber} — {a.accountHolder}
                {a.balance != null
                  ? ` (${a.currency} ${Number(a.balance).toLocaleString("en-IN")})`
                  : ""}
              </option>
            ))}
          </select>
          {sourceAcc && (
            <p className="mt-1 text-xs text-slate-400">
              {sourceAcc.accountType} · {sourceAcc.currency} ·{" "}
              {sourceAcc.status}
              {sourceAcc.balance != null && (
                <span className="ml-2 font-medium text-slate-600">
                  Balance: {sourceAcc.currency}{" "}
                  {Number(sourceAcc.balance).toLocaleString("en-IN")}
                </span>
              )}
            </p>
          )}
        </label>

        {/* Destination account */}
        <label className="text-sm sm:col-span-2">
          <span className="mb-1 block font-medium text-slate-700">
            Destination account
          </span>
          <select
            value={form.destinationAccount}
            onChange={(e) => handleDestinationAccountChange(e.target.value)}
            required
            className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-slate-900 outline-none ring-orange-500/30 focus:ring"
          >
            <option value="">Select destination account…</option>
            {destOptions.map((a) => (
              <option key={a.id} value={a.accountNumber}>
                {a.accountNumber} — {a.accountHolder} ({a.currency})
              </option>
            ))}
          </select>
        </label>

        {/* Amount */}
        <label className="text-sm">
          <span className="mb-1 block font-medium text-slate-700">Amount</span>
          <input
            type="number"
            min="0.01"
            step="0.01"
            value={form.amount}
            onChange={set("amount")}
            required
            className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-slate-900 outline-none ring-orange-500/30 focus:ring"
          />
        </label>

        {/* Currency */}
        <label className="text-sm">
          <span className="mb-1 block font-medium text-slate-700">
            Currency
          </span>
          <select
            value={form.currency}
            onChange={set("currency")}
            className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-slate-900 outline-none ring-orange-500/30 focus:ring"
          >
            {CURRENCIES.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
        </label>

         {/* Countries are auto-fetched from source and destination accounts */}
      </div>

      {/* Live currency conversion preview */}
      {showExchangePreview && (exchangeLoading || exchange || exchangeError) && (
        <div className="mt-4 rounded-lg bg-slate-50 px-3 py-2 text-sm dark:bg-slate-800/50">
          {exchangeLoading && (
            <span className="text-slate-500">Fetching live exchange rate…</span>
          )}
          {!exchangeLoading && exchange && (
            <span className="text-slate-700 dark:text-slate-200">
              Recipient receives approx.{" "}
              <span className="font-semibold">
                {exchange.toCurrency} {exchange.convertedAmount.toLocaleString("en-IN")}
              </span>{" "}
              <span className="text-xs text-slate-400">
                (1 {exchange.fromCurrency} = {exchange.exchangeRate} {exchange.toCurrency})
              </span>
            </span>
          )}
          {!exchangeLoading && exchangeError && (
            <span className="text-amber-600">
              Live exchange rate unavailable: {exchangeError}
            </span>
          )}
        </div>
      )}

      {/* Schedule payment */}
      <div className="mt-4 rounded-lg border border-slate-200 p-3">
        <label className="flex items-center gap-2 text-sm font-medium text-slate-700">
          <input
            type="checkbox"
            checked={isScheduled}
            onChange={(e) => {
              setIsScheduled(e.target.checked);
              if (!e.target.checked) setScheduledAt("");
            }}
            className="h-4 w-4 rounded border-slate-300 text-orange-600 focus:ring-orange-500"
          />
          Schedule this payment for later
        </label>
        {isScheduled && (
          <label className="mt-3 block text-sm">
            <span className="mb-1 block font-medium text-slate-700">
              Release date &amp; time (IST)
            </span>
            <input
              type="datetime-local"
              value={scheduledAt}
              onChange={(e) => setScheduledAt(e.target.value)}
              required={isScheduled}
              className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-slate-900 outline-none ring-orange-500/30 focus:ring sm:w-64"
            />
            <span className="mt-1 block text-xs text-slate-400">
              The payment will be held and automatically processed at this
              Indian Standard Time.
            </span>
          </label>
        )}
      </div>

      {error && (
        <p className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </p>
      )}

      <button
        type="submit"
        disabled={isSubmitting}
        className="mt-5 inline-flex items-center justify-center rounded-lg bg-orange-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-orange-700 disabled:cursor-not-allowed disabled:opacity-60"
      >
        {isSubmitting ? "Sending OTP…" : "Continue — Send OTP"}
      </button>
    </form>
  );
}
