"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { getAccounts, getPayments, createPayment, sendOtp, verifyOtp } from "@/lib/api";
import type { Account, Payment, CreatePaymentInput } from "@/lib/types";
import { formatCurrency, formatDateTime } from "@/lib/format";
import { StatusBadge } from "@/components/status-badge";
import { TransactionCharts } from "@/components/transaction-charts";
import { useUserStore } from "@/lib/user-store";
import type { AppUser, UserRole } from "@/lib/user-store";
import { listOnboardingUsers } from "@/lib/onboarding-api";

type Tab = "sent" | "received";

function statCard(label: string, value: string | number) {
  return (
    <article className="panel rounded-2xl p-5">
      <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
        {label}
      </p>
      <p className="mt-1 text-2xl font-bold text-slate-900">{value}</p>
    </article>
  );
}

function sumAmount(rows: Payment[]): number {
  return rows.reduce((s, p) => s + (Number.parseFloat(p.amount) || 0), 0);
}

export default function AccountsPage() {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [payments, setPayments] = useState<Payment[]>([]);
  const [selected, setSelected] = useState<Account | null>(null);
  const [tab, setTab] = useState<Tab>("sent");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const { currentUser, mergeUsers } = useUserStore();
  const isAdmin = !currentUser || currentUser.role === "admin";

  // Send payment form state
  const [showSend, setShowSend] = useState(false);
  const [sendForm, setSendForm] = useState<Partial<CreatePaymentInput>>({
    currency: "INR",
    originCountry: "IN",
    destinationCountry: "IN",
    amount: 0,
    destinationAccount: "",
  });
  const [sending, setSending] = useState(false);
  const [sendError, setSendError] = useState<string | null>(null);
  const [sendSuccess, setSendSuccess] = useState<string | null>(null);

  // OTP flow for the accounts page send form
  const [sendStep, setSendStep] = useState<"form" | "otp">("form");
  const [sendOtpValue, setSendOtpValue] = useState("");
  const [sendOtpError, setSendOtpError] = useState<string | null>(null);
  const [sendVerifying, setSendVerifying] = useState(false);
  const [sendMaskedEmail, setSendMaskedEmail] = useState("");

  useEffect(() => {
    let cancelled = false;
    Promise.all([getAccounts(), getPayments(), listOnboardingUsers()])
      .then(([accs, pays, customers]) => {
        if (cancelled) return;
        setAccounts(accs);
        setPayments(pays);
        setError(null);

        // Sync onboarding customers into the user-store so seed users appear
        // in the user-switcher without requiring manual add.
        mergeUsers(
          customers.map((c) => ({
            customerId: c.customerId,
            name: c.fullName,
            email: c.email,
            phone: c.phone,
            accountNumber: c.accountNumber,
            role: c.role.toLowerCase() as UserRole,
          }) satisfies AppUser)
        );

        // Pre-select: members see only their own account; admins see the first
        if (currentUser?.role === "member") {
          const mine = accs.find((a) => a.accountNumber === currentUser.accountNumber);
          setSelected(mine ?? accs[0] ?? null);
        } else {
          if (accs.length > 0) setSelected(accs[0]);
        }
      })
      .catch((e) => {
        if (!cancelled)
          setError(e instanceof Error ? e.message : "Unable to load data");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const sent = useMemo(
    () =>
      selected
        ? payments.filter((p) => p.sourceAccount === selected.accountNumber)
        : [],
    [payments, selected],
  );

  const received = useMemo(
    () =>
      selected
        ? payments.filter(
            (p) => p.destinationAccount === selected.accountNumber,
          )
        : [],
    [payments, selected],
  );

  const rows = tab === "sent" ? sent : received;

  /** Masks an email for display: "payer@example.com" → "pa***@example.com" */
  const maskEmail = (email: string): string => {
    const [local, domain] = email.split("@");
    if (!domain) return email;
    return `${local.slice(0, Math.min(2, local.length))}***@${domain}`;
  };

  // Step 1: validate form then send OTP
  const handleSend = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selected) return;
    if (!selected.email) {
      setSendError("No email address is registered on this account. Update the account before sending a payment.");
      return;
    }
    setSending(true);
    setSendError(null);
    setSendSuccess(null);
    try {
      await sendOtp(selected.email, selected.accountNumber);
      setSendMaskedEmail(maskEmail(selected.email));
      setSendStep("otp");
    } catch (err) {
      setSendError(err instanceof Error ? err.message : "Failed to send OTP");
    } finally {
      setSending(false);
    }
  };

  // Step 2: verify OTP then create payment
  const handleSendOtpVerify = async () => {
    if (!selected) return;
    if (sendOtpValue.length !== 4) {
      setSendOtpError("Please enter the 4-digit OTP.");
      return;
    }
    setSendOtpError(null);
    setSendVerifying(true);
    try {
      const result = await verifyOtp(selected.email!, sendOtpValue);
      if (!result.valid) {
        setSendOtpError(result.message ?? "Invalid or expired OTP. Try again.");
        return;
      }
      const created = await createPayment({
        amount: sendForm.amount ?? 0,
        currency: sendForm.currency ?? "INR",
        sourceAccount: selected.accountNumber,
        destinationAccount: sendForm.destinationAccount ?? "",
        originCountry: sendForm.originCountry ?? "IN",
        destinationCountry: sendForm.destinationCountry ?? "IN",
        payerEmail: selected.email,
      });
      setPayments((prev) => [created, ...prev]);
      setSendSuccess(`Payment ${created.reference ?? `#${created.id}`} submitted successfully.`);
      setSendForm((f) => ({ ...f, amount: 0, destinationAccount: "" }));
      setSendStep("form");
      setSendOtpValue("");
      setSendMaskedEmail("");
      setShowSend(false);
    } catch (err) {
      setSendOtpError(err instanceof Error ? err.message : "Unable to submit payment");
    } finally {
      setSendVerifying(false);
    }
  };

  const otherAccounts = accounts.filter(
    (a) => a.accountNumber !== selected?.accountNumber,
  );

  // Members can only view/send from their own account
  const visibleAccounts = useMemo(
    () =>
      isAdmin
        ? accounts
        : accounts.filter((a) => a.accountNumber === currentUser?.accountNumber),
    [accounts, isAdmin, currentUser],
  );

  if (loading) {
    return (
      <div className="mx-auto max-w-7xl px-6 py-16 text-center text-sm text-slate-500">
        Loading accounts…
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl space-y-8 px-6 py-10 lg:px-8">
      {/* Header */}
      <header className="space-y-1">
        <p className="text-sm font-semibold uppercase tracking-wide text-orange-700">
          Account View
        </p>
        <h1 className="text-3xl font-bold tracking-tight text-slate-900">
          Customer account profile
        </h1>
        <p className="text-slate-600">
          Select an account to view its profile, payment history, and send a new
          payment.
        </p>
      </header>

      {error && (
        <p className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </p>
      )}

      {/* Account selector — only shown to admins; members always see their own account */}
      {isAdmin && (
      <section className="panel rounded-2xl p-5">
        <label
          htmlFor="account-select"
          className="mb-1 block text-sm font-medium text-slate-700"
        >
          Select account
        </label>
        <select
          id="account-select"
          className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 sm:max-w-xl"
          value={selected?.accountNumber ?? ""}
          onChange={(e) => {
            const acc = visibleAccounts.find(
              (a) => a.accountNumber === e.target.value,
            );
            setSelected(acc ?? null);
            setShowSend(false);
            setSendSuccess(null);
            setSendError(null);
          }}
        >
          {visibleAccounts.map((a) => (
            <option key={a.id} value={a.accountNumber}>
              {a.accountNumber} — {a.accountHolder}
            </option>
          ))}
        </select>
      </section>
      )}

      {selected && (
        <>
          {/* Profile card */}
          <section className="panel rounded-2xl p-6">
            <div className="flex flex-col gap-6 sm:flex-row sm:items-start sm:justify-between">
              <div className="flex items-center gap-4">
                {/* Avatar */}
                <span className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-orange-100 text-2xl font-bold text-orange-700">
                  {selected.accountHolder.charAt(0).toUpperCase()}
                </span>
                <div>
                  <h2 className="text-xl font-bold text-slate-900">
                    {selected.accountHolder}
                  </h2>
                  <p className="font-mono text-sm text-slate-500">
                    {selected.accountNumber}
                  </p>
                  <div className="mt-1 flex flex-wrap gap-2">
                    <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600">
                      {selected.accountType}
                    </span>
                    <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600">
                      {selected.currency}
                    </span>
                    {selected.balance != null && (
                      <span className="rounded-full bg-orange-50 px-2 py-0.5 text-xs font-semibold text-orange-700 ring-1 ring-orange-200">
                        Balance: {selected.currency}{" "}
                        {Number(selected.balance).toLocaleString("en-IN")}
                      </span>
                    )}
                    <span
                      className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                        selected.status === "ACTIVE"
                          ? "bg-emerald-100 text-emerald-700"
                          : "bg-red-100 text-red-700"
                      }`}
                    >
                      {selected.status}
                    </span>
                  </div>
                </div>
              </div>

              <button
                onClick={() => {
                  setShowSend((v) => !v);
                  setSendError(null);
                  setSendSuccess(null);
                  setSendStep("form");
                  setSendOtpValue("");
                  setSendOtpError(null);
                }}
                className="self-start rounded-xl bg-orange-600 px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-orange-700"
              >
                {showSend ? "Cancel" : "Send payment"}
              </button>
            </div>

            {/* Extra account metadata */}
            <div className="mt-5 grid grid-cols-2 gap-3 border-t border-black/5 pt-5 sm:grid-cols-4 text-sm">
              {selected.bankName && (
                <div>
                  <p className="text-xs text-slate-500">Bank</p>
                  <p className="font-medium text-slate-800">
                    {selected.bankName}
                  </p>
                </div>
              )}
              {selected.ifscCode && (
                <div>
                  <p className="text-xs text-slate-500">IFSC</p>
                  <p className="font-mono font-medium text-slate-800">
                    {selected.ifscCode}
                  </p>
                </div>
              )}
              {selected.swiftCode && (
                <div>
                  <p className="text-xs text-slate-500">SWIFT</p>
                  <p className="font-mono font-medium text-slate-800">
                    {selected.swiftCode}
                  </p>
                </div>
              )}
              <div>
                <p className="text-xs text-slate-500">Member since</p>
                <p className="font-medium text-slate-800">
                  {formatDateTime(selected.createdAt)}
                </p>
              </div>
            </div>
          </section>

          {/* Send payment inline form */}
          {showSend && (
            <section className="panel rounded-2xl p-6 border-l-4 border-orange-400">
              <h3 className="text-base font-semibold text-slate-900">
                Send a payment
              </h3>
              <p className="mt-0.5 text-sm text-slate-500">
                Funds will be debited from{" "}
                <span className="font-medium">{selected.accountNumber}</span>.
              </p>
              {sendError && (
                <p className="mt-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
                  {sendError}
                </p>
              )}

              {/* OTP verification step */}
              {sendStep === "otp" ? (
                <div className="mt-4">
                  <p className="text-sm text-slate-600">
                    A 4-digit OTP has been sent to{" "}
                    <span className="font-medium text-slate-800">{sendMaskedEmail}</span>.
                    Enter it below to authorise the payment.
                  </p>
                  <div className="mt-3">
                    <input
                      type="text"
                      inputMode="numeric"
                      maxLength={4}
                      pattern="\d{4}"
                      placeholder="_ _ _ _"
                      value={sendOtpValue}
                      onChange={(e) => {
                        setSendOtpValue(e.target.value.replace(/\D/g, "").slice(0, 4));
                        setSendOtpError(null);
                      }}
                      className="w-36 rounded-lg border border-slate-200 bg-white px-3 py-2 text-center text-2xl tracking-widest text-slate-900 outline-none ring-orange-500/30 focus:ring"
                    />
                  </div>
                  {sendOtpError && (
                    <p className="mt-2 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
                      {sendOtpError}
                    </p>
                  )}
                  <div className="mt-4 flex gap-3">
                    <button
                      type="button"
                      onClick={handleSendOtpVerify}
                      disabled={sendVerifying || sendOtpValue.length !== 4}
                      className="rounded-xl bg-orange-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-orange-700 disabled:opacity-60"
                    >
                      {sendVerifying ? "Verifying…" : "Verify & Pay"}
                    </button>
                    <button
                      type="button"
                      onClick={() => { setSendStep("form"); setSendOtpValue(""); setSendOtpError(null); setSendError(null); }}
                      className="text-sm text-slate-500 hover:text-slate-700"
                    >
                      Cancel
                    </button>
                  </div>
                </div>
              ) : (
                <form
                  onSubmit={handleSend}
                  className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2"
                >
                <label className="text-sm">
                  <span className="mb-1 block font-medium text-slate-700">
                    Destination account
                  </span>
                  <select
                    className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2"
                    value={sendForm.destinationAccount}
                    onChange={(e) =>
                      setSendForm((f) => ({
                        ...f,
                        destinationAccount: e.target.value,
                      }))
                    }
                    required
                  >
                    <option value="">Select destination…</option>
                    {otherAccounts.map((a) => (
                      <option key={a.id} value={a.accountNumber}>
                        {a.accountNumber} — {a.accountHolder}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="text-sm">
                  <span className="mb-1 block font-medium text-slate-700">
                    Amount
                  </span>
                  <input
                    type="number"
                    min="1"
                    step="0.01"
                    className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2"
                    value={sendForm.amount || ""}
                    onChange={(e) =>
                      setSendForm((f) => ({
                        ...f,
                        amount: Number(e.target.value),
                      }))
                    }
                    required
                  />
                </label>

                <label className="text-sm">
                  <span className="mb-1 block font-medium text-slate-700">
                    Currency
                  </span>
                  <select
                    className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2"
                    value={sendForm.currency}
                    onChange={(e) =>
                      setSendForm((f) => ({ ...f, currency: e.target.value }))
                    }
                  >
                    {["INR", "USD", "EUR", "GBP", "SGD", "AED"].map((c) => (
                      <option key={c} value={c}>
                        {c}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="text-sm">
                  <span className="mb-1 block font-medium text-slate-700">
                    Destination country
                  </span>
                  <select
                    className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2"
                    value={sendForm.destinationCountry}
                    onChange={(e) =>
                      setSendForm((f) => ({
                        ...f,
                        destinationCountry: e.target.value,
                      }))
                    }
                  >
                    {[
                      ["IN", "India"],
                      ["US", "United States"],
                      ["GB", "United Kingdom"],
                      ["SG", "Singapore"],
                      ["AE", "UAE"],
                      ["EU", "Europe"],
                    ].map(([code, name]) => (
                      <option key={code} value={code}>
                        {name} ({code})
                      </option>
                    ))}
                  </select>
                </label>

                <div className="sm:col-span-2 flex gap-3">
                  <button
                    type="submit"
                    disabled={sending}
                    className="rounded-xl bg-orange-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-orange-700 disabled:opacity-60"
                  >
                    {sending ? "Sending OTP…" : "Continue — Send OTP"}
                  </button>
                </div>
              </form>
              )}
            </section>
          )}

          {sendSuccess && (
            <p className="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
              {sendSuccess}
            </p>
          )}

          {/* Stats */}
          <section className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            {statCard("Payments Sent", sent.length)}
            {statCard(
              "Total Sent",
              formatCurrency(sumAmount(sent), selected.currency),
            )}
            {statCard("Payments Received", received.length)}
            {statCard(
              "Total Received",
              formatCurrency(sumAmount(received), selected.currency),
            )}
          </section>

          {/* Transaction Charts */}
          <TransactionCharts
            payments={payments}
            selectedAccountNumber={selected.accountNumber}
          />

          {/* Transactions tabs */}
          <section className="panel overflow-hidden rounded-2xl">
            <div className="border-b border-black/5 px-6 py-4 flex items-center gap-6">
              <h2 className="text-lg font-semibold text-slate-900 mr-4">
                Transactions
              </h2>
              {(["sent", "received"] as Tab[]).map((t) => (
                <button
                  key={t}
                  onClick={() => setTab(t)}
                  className={`pb-0.5 text-sm font-medium capitalize border-b-2 transition-colors ${
                    tab === t
                      ? "border-orange-500 text-orange-700"
                      : "border-transparent text-slate-500 hover:text-slate-700"
                  }`}
                >
                  {t === "sent"
                    ? `Sent (${sent.length})`
                    : `Received (${received.length})`}
                </button>
              ))}
            </div>

            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-black/5">
                <thead className="bg-slate-50">
                  <tr>
                    {[
                      "Reference",
                      tab === "sent" ? "To" : "From",
                      "Amount",
                      "Status",
                      "Date",
                      "",
                    ].map((h, i) => (
                      <th
                        key={i}
                        className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500"
                      >
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-black/5">
                  {rows.length === 0 ? (
                    <tr>
                      <td
                        colSpan={6}
                        className="px-6 py-10 text-center text-sm text-slate-400"
                      >
                        No {tab} payments for this account.
                      </td>
                    </tr>
                  ) : (
                    rows.map((p) => (
                      <tr key={p.id} className="hover:bg-slate-50/60">
                        <td className="px-6 py-4 text-sm">
                          <p className="font-medium text-slate-900">
                            {p.reference ?? `Payment #${p.id}`}
                          </p>
                          <p className="text-xs text-slate-400 font-mono">
                            {p.idempotencyKey}
                          </p>
                        </td>
                        <td className="px-6 py-4 text-sm text-slate-600">
                          {tab === "sent"
                            ? p.destinationAccount
                            : p.sourceAccount}
                        </td>
                        <td className="px-6 py-4 text-sm font-semibold text-slate-900">
                          {formatCurrency(p.amount, p.currency)}
                        </td>
                        <td className="px-6 py-4">
                          <StatusBadge status={p.status} />
                        </td>
                        <td className="px-6 py-4 text-sm text-slate-500">
                          {formatDateTime(p.createdAt)}
                        </td>
                        <td className="px-6 py-4 text-sm">
                          <Link
                            href={`/payments/${p.id}`}
                            className="font-medium text-orange-700 hover:text-orange-800"
                          >
                            Open
                          </Link>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </section>
        </>
      )}
    </div>
  );
}
