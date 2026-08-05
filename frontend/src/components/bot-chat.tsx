"use client";

import React, { useEffect, useRef, useState } from "react";
import {
  Bot,
  User,
  Send,
  Loader2,
  CheckCircle2,
  XCircle,
  Wallet,
  Users,
  Search,
  RotateCcw,
  Ban,
  ArrowRightLeft,
  Sparkles,
  KeyRound,
} from "lucide-react";
import { formatCurrency } from "@/lib/format";
import { StatusBadge } from "@/components/status-badge";
import { getAccounts, sendOtp, verifyOtp } from "@/lib/api";

type MsgKind = "text" | "account" | "accounts" | "payment" | "error";

type Msg = {
  role: "user" | "bot";
  kind: MsgKind;
  text?: string;
  account?: AccountInfo;
  accounts?: AccountInfo[];
  payment?: PaymentInfo;
};

type PaymentPayload = {
  amount?: number;
  currency?: string;
  sourceAccount?: string;
  destinationAccount?: string;
  paymentId?: string;
  accountNumber?: string;
  raw?: string;
};

type BotCommand = {
  type:
    | "create_payment"
    | "retry_payment"
    | "cancel_payment"
    | "query_payments"
    | "check_balance"
    | "list_accounts"
    | "payment_status"
    | "unknown";
  payload: PaymentPayload;
  confidence: number;
  summary?: string;
  requiresConfirmation?: boolean;
  readOnly?: boolean;
  source?: "slm" | "rules";
};

type AccountInfo = {
  accountNumber: string;
  accountHolder: string;
  currency: string;
  balance: number;
  status: string;
};

type PaymentInfo = {
  paymentId: number;
  amount: number;
  currency: string;
  sourceAccount: string;
  destinationAccount: string;
  status: string;
  errorMessage?: string;
};

const TYPE_LABELS: Record<string, string> = {
  create_payment: "Create Payment",
  retry_payment: "Retry Payment",
  cancel_payment: "Cancel Payment",
  query_payments: "Query Payments",
  check_balance: "Check Balance",
  list_accounts: "List Accounts",
  payment_status: "Payment Status",
};

function maskEmail(email: string): string {
  const [local, domain] = email.split("@");
  if (!domain) return email;
  const visible = local.slice(0, Math.min(2, local.length));
  return `${visible}***@${domain}`;
}

const TYPE_ICONS: Record<string, React.ComponentType<{ className?: string }>> = {
  create_payment: ArrowRightLeft,
  retry_payment: RotateCcw,
  cancel_payment: Ban,
  check_balance: Wallet,
  list_accounts: Users,
  payment_status: Search,
};

const SUGGESTIONS = [
  { label: "Check balance", text: "What is the balance of ACC-10001?" },
  { label: "List accounts", text: "List all accounts" },
  { label: "Payment status", text: "What is the status of payment 1?" },
  {
    label: "Create payment",
    text: "Create payment of 5000 INR from ACC-10001 to ACC-10002",
  },
];

export default function BotChat() {
  const [text, setText] = useState("");
  const [msgs, setMsgs] = useState<Msg[]>([]);
  const [loading, setLoading] = useState(false);
  const [pendingCommand, setPendingCommand] = useState<BotCommand | null>(null);
  const [confirming, setConfirming] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  // OTP gate state (used only for create_payment commands)
  const [otpStep, setOtpStep] = useState(false);
  const [otpValue, setOtpValue] = useState("");
  const [otpError, setOtpError] = useState<string | null>(null);
  const [isVerifying, setIsVerifying] = useState(false);
  const [maskedEmail, setMaskedEmail] = useState("");
  const [resolvedEmail, setResolvedEmail] = useState("");

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: "smooth" });
  }, [msgs, pendingCommand]);

  function addBotText(t: string) {
    setMsgs((m) => [...m, { role: "bot", kind: "text", text: t }]);
  }

  function addBotError(t: string) {
    setMsgs((m) => [...m, { role: "bot", kind: "error", text: t }]);
  }

  async function send(overrideText?: string) {
    const value = overrideText ?? text;
    if (!value.trim()) return;
    setMsgs((m) => [...m, { role: "user", kind: "text", text: value }]);
    setText("");
    setLoading(true);
    setPendingCommand(null);
    try {
      const resp = await fetch("/api/bot/nl", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ text: value }),
      });
      const json = await resp.json();
      const intent: BotCommand | undefined = json.intent;

      if (!intent || intent.type === "unknown") {
        addBotText(
          "Sorry, I couldn't understand that request. Try one of the suggestions below, or phrase it like \"Create payment of 5000 INR from ACC-10001 to ACC-10002\"."
        );
        return;
      }

      if (intent.type === "query_payments") {
        addBotText("Please use the Payments page to browse and filter transactions.");
        return;
      }

      // Read-only lookups (balance/status/list) have already been resolved
      // by the bot service — just render the result, no confirmation needed.
      if (intent.readOnly) {
        if (json.error) {
          addBotError(json.error);
        } else if (intent.type === "check_balance") {
          setMsgs((m) => [...m, { role: "bot", kind: "account", account: json.result as AccountInfo }]);
        } else if (intent.type === "list_accounts") {
          setMsgs((m) => [...m, { role: "bot", kind: "accounts", accounts: (json.result as AccountInfo[]) || [] }]);
        } else if (intent.type === "payment_status") {
          setMsgs((m) => [...m, { role: "bot", kind: "payment", payment: json.result as PaymentInfo }]);
        }
        return;
      }

      // Every state-changing action (create/retry/cancel) always requires
      // an explicit confirmation click before it's queued for execution.
      setPendingCommand(intent);
    } catch {
      addBotError("Error contacting bot service");
    } finally {
      setLoading(false);
    }
  }

  function resetOtpState() {
    setOtpStep(false);
    setOtpValue("");
    setOtpError(null);
    setMaskedEmail("");
    setResolvedEmail("");
  }

  async function queueCommand() {
    if (!pendingCommand) return;
    setConfirming(true);
    try {
      const endpoint = pendingCommand.requiresConfirmation ? "/api/bot/confirm" : "/api/bot/execute";
      const body = pendingCommand.requiresConfirmation
        ? { command: pendingCommand, approver: "ui-user" }
        : { command: pendingCommand };
      const resp = await fetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
      const json = await resp.json();
      if (resp.ok) {
        addBotText(`Queued: ${TYPE_LABELS[pendingCommand.type] ?? pendingCommand.type} — it will be processed shortly.`);
      } else {
        addBotError(`Failed to queue: ${json.error || json.message || "unknown error"}`);
      }
    } catch {
      addBotError("Error submitting command");
    } finally {
      resetOtpState();
      setPendingCommand(null);
      setConfirming(false);
    }
  }

  async function initiateOtp() {
    if (!pendingCommand?.payload.sourceAccount) {
      addBotError("Source account is missing. Cannot send OTP.");
      return;
    }
    setConfirming(true);
    try {
      const accounts = await getAccounts();
      const src = accounts.find((a) => a.accountNumber === pendingCommand.payload.sourceAccount);
      if (!src?.email) {
        addBotError("No email address is registered on this account. Please update the account before proceeding.");
        setPendingCommand(null);
        return;
      }
      await sendOtp(src.email, pendingCommand.payload.sourceAccount);
      setResolvedEmail(src.email);
      setMaskedEmail(maskEmail(src.email));
      setOtpStep(true);
      setOtpValue("");
      setOtpError(null);
    } catch {
      addBotError("Failed to send OTP. Please try again.");
    } finally {
      setConfirming(false);
    }
  }

  async function handleBotOtpVerify() {
    if (otpValue.length !== 4) {
      setOtpError("Please enter the 4-digit OTP.");
      return;
    }
    setOtpError(null);
    setIsVerifying(true);
    try {
      const result = await verifyOtp(resolvedEmail, otpValue);
      if (!result.valid) {
        setOtpError(result.message ?? "Invalid or expired OTP. Try again.");
        return;
      }
      await queueCommand();
    } catch {
      setOtpError("Verification failed. Please try again.");
    } finally {
      setIsVerifying(false);
    }
  }

  async function confirmPending() {
    if (!pendingCommand) return;
    if (pendingCommand.type === "create_payment") {
      await initiateOtp();
    } else {
      await queueCommand();
    }
  }

  function cancelPending() {
    resetOtpState();
    setPendingCommand(null);
    addBotText("Cancelled — nothing was submitted.");
  }

  return (
    <div className="mx-auto flex h-[70vh] max-w-3xl flex-col overflow-hidden rounded-2xl panel">
      <div className="flex items-center gap-3 border-b border-black/5 px-5 py-4">
        <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-orange-500/10 text-orange-600">
          <Bot className="h-5 w-5" />
        </span>
        <div>
          <p className="text-sm font-semibold text-slate-900">RupeeX Assistant</p>
          <p className="text-xs text-slate-500">Payments, balances, and account lookups in plain English</p>
        </div>
      </div>

      <div ref={scrollRef} className="flex-1 space-y-3 overflow-y-auto bg-slate-50/60 px-5 py-4">
        {msgs.length === 0 && (
          <div className="flex h-full flex-col items-center justify-center gap-4 text-center">
            <span className="flex h-12 w-12 items-center justify-center rounded-full bg-orange-500/10 text-orange-600">
              <Sparkles className="h-6 w-6" />
            </span>
            <p className="max-w-xs text-sm text-slate-500">
              Ask me to create or manage payments, check an account balance, or look up a payment status.
            </p>
            <div className="flex flex-wrap justify-center gap-2">
              {SUGGESTIONS.map((s) => (
                <button
                  key={s.label}
                  onClick={() => send(s.text)}
                  disabled={loading}
                  className="rounded-full border border-orange-200 bg-white px-3 py-1.5 text-xs font-medium text-orange-700 shadow-sm transition hover:bg-orange-50 disabled:opacity-50"
                >
                  {s.label}
                </button>
              ))}
            </div>
          </div>
        )}

        {msgs.map((m, i) => (
          <ChatBubble key={i} msg={m} />
        ))}

        {loading && (
          <div className="flex items-center gap-2 text-xs text-slate-400">
            <Loader2 className="h-3.5 w-3.5 animate-spin" />
            Thinking…
          </div>
        )}

        {pendingCommand && !otpStep && (
          <TransactionPreview
            command={pendingCommand}
            loading={confirming}
            onConfirm={confirmPending}
            onCancel={cancelPending}
          />
        )}

        {pendingCommand && otpStep && (
          <BotOtpGate
            maskedEmail={maskedEmail}
            otpValue={otpValue}
            otpError={otpError}
            isVerifying={isVerifying}
            onOtpChange={(val) => { setOtpValue(val); setOtpError(null); }}
            onVerify={handleBotOtpVerify}
            onCancel={cancelPending}
          />
        )}
      </div>

      <div className="border-t border-black/5 bg-white px-4 py-3">
        <div className="flex items-center gap-2 rounded-full border border-black/10 bg-slate-50 px-2 py-1.5 focus-within:border-orange-300 focus-within:ring-2 focus-within:ring-orange-100">
          <input
            value={text}
            onChange={(e) => setText(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && !loading && send()}
            className="flex-1 bg-transparent px-3 py-1.5 text-sm outline-none placeholder:text-slate-400"
            placeholder="e.g. Check balance of ACC-10001, or create payment of 5000 INR from ACC-10001 to ACC-10002"
          />
          <button
            onClick={() => send()}
            disabled={loading || !text.trim()}
            className="flex h-8 w-8 items-center justify-center rounded-full bg-orange-600 text-white transition hover:bg-orange-700 disabled:cursor-not-allowed disabled:opacity-40"
            aria-label="Send"
          >
            {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
          </button>
        </div>
      </div>
    </div>
  );
}

function ChatBubble({ msg }: { msg: Msg }) {
  const isUser = msg.role === "user";
  const Avatar = isUser ? User : Bot;

  return (
    <div className={`flex items-end gap-2 ${isUser ? "flex-row-reverse" : "flex-row"}`}>
      <span
        className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-full ${
          isUser ? "bg-slate-800 text-white" : "bg-orange-500/10 text-orange-600"
        }`}
      >
        <Avatar className="h-3.5 w-3.5" />
      </span>

      <div className={`max-w-[85%] ${isUser ? "items-end" : "items-start"} flex flex-col gap-1`}>
        {msg.kind === "text" && (
          <div
            className={`rounded-2xl px-3.5 py-2 text-sm shadow-sm ${
              isUser ? "rounded-br-sm bg-slate-900 text-white" : "rounded-bl-sm bg-white text-slate-800"
            }`}
          >
            <pre className="whitespace-pre-wrap break-words font-sans">{msg.text}</pre>
          </div>
        )}

        {msg.kind === "error" && (
          <div className="flex items-center gap-2 rounded-2xl rounded-bl-sm border border-red-200 bg-red-50 px-3.5 py-2 text-sm text-red-700 shadow-sm">
            <XCircle className="h-4 w-4 shrink-0" />
            {msg.text}
          </div>
        )}

        {msg.kind === "account" && msg.account && <AccountCard account={msg.account} />}
        {msg.kind === "accounts" && msg.accounts && <AccountListCard accounts={msg.accounts} />}
        {msg.kind === "payment" && msg.payment && <PaymentCard payment={msg.payment} />}
      </div>
    </div>
  );
}

function AccountCard({ account }: { account: AccountInfo }) {
  return (
    <div className="w-72 max-w-full rounded-2xl rounded-bl-sm border border-black/5 bg-white p-3.5 shadow-sm">
      <div className="flex items-center gap-2 text-xs font-medium text-slate-500">
        <Wallet className="h-3.5 w-3.5 text-orange-600" />
        {account.accountNumber} · {account.accountHolder}
      </div>
      <p className="mt-1.5 text-xl font-semibold tracking-tight text-slate-900">
        {formatCurrency(account.balance, account.currency)}
      </p>
      <div className="mt-2">
        <StatusBadge status={account.status} />
      </div>
    </div>
  );
}

function AccountListCard({ accounts }: { accounts: AccountInfo[] }) {
  if (!accounts.length) {
    return (
      <div className="rounded-2xl rounded-bl-sm bg-white px-3.5 py-2 text-sm text-slate-500 shadow-sm">
        No accounts found.
      </div>
    );
  }
  return (
    <div className="w-80 max-w-full divide-y divide-black/5 rounded-2xl rounded-bl-sm border border-black/5 bg-white shadow-sm">
      {accounts.map((a) => (
        <div key={a.accountNumber} className="flex items-center justify-between gap-2 px-3.5 py-2.5">
          <div className="min-w-0">
            <p className="truncate text-sm font-medium text-slate-900">{a.accountNumber}</p>
            <p className="truncate text-xs text-slate-500">{a.accountHolder}</p>
          </div>
          <div className="shrink-0 text-right">
            <p className="text-sm font-semibold text-slate-900">{formatCurrency(a.balance, a.currency)}</p>
            <StatusBadge status={a.status} />
          </div>
        </div>
      ))}
    </div>
  );
}

function PaymentCard({ payment }: { payment: PaymentInfo }) {
  return (
    <div className="w-72 max-w-full rounded-2xl rounded-bl-sm border border-black/5 bg-white p-3.5 shadow-sm">
      <div className="flex items-center justify-between gap-2">
        <span className="flex items-center gap-1.5 text-xs font-medium text-slate-500">
          <Search className="h-3.5 w-3.5 text-orange-600" />
          Payment #{payment.paymentId}
        </span>
        <StatusBadge status={payment.status} />
      </div>
      <p className="mt-1.5 text-lg font-semibold tracking-tight text-slate-900">
        {formatCurrency(payment.amount, payment.currency)}
      </p>
      <p className="mt-1 text-xs text-slate-500">
        {payment.sourceAccount} <ArrowRightLeft className="mx-1 inline h-3 w-3" /> {payment.destinationAccount}
      </p>
      {payment.errorMessage && (
        <p className="mt-1.5 rounded-lg bg-red-50 px-2 py-1 text-xs text-red-700">{payment.errorMessage}</p>
      )}
    </div>
  );
}

function Row({ label, value }: { label: string; value?: React.ReactNode }) {
  if (value === undefined || value === null || value === "") return null;
  return (
    <div className="flex justify-between py-1.5 text-sm">
      <span className="text-slate-500">{label}</span>
      <span className="font-medium text-slate-900">{value}</span>
    </div>
  );
}

function TransactionPreview({
  command,
  loading,
  onConfirm,
  onCancel,
}: {
  command: BotCommand;
  loading: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  const { payload, requiresConfirmation, confidence, source } = command;
  const amountDisplay =
    payload.amount !== undefined ? formatCurrency(payload.amount, payload.currency || "INR") : undefined;
  const Icon = TYPE_ICONS[command.type] ?? ArrowRightLeft;

  return (
    <div className="ml-9 w-[22rem] max-w-full rounded-2xl border border-orange-200 bg-orange-50/70 p-4 shadow-sm">
      <div className="mb-2 flex items-center justify-between">
        <span className="flex items-center gap-1.5 text-sm font-semibold text-orange-900">
          <Icon className="h-4 w-4" />
          Review {TYPE_LABELS[command.type] ?? command.type}
        </span>
        {requiresConfirmation && (
          <span className="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800">
            High value
          </span>
        )}
      </div>

      <div className="divide-y divide-orange-100 rounded-xl bg-white/80 px-3">
        <Row label="Amount" value={amountDisplay} />
        <Row label="From account" value={payload.sourceAccount} />
        <Row label="To account" value={payload.destinationAccount} />
        <Row label="Payment ID" value={payload.paymentId} />
        <Row label="Parsed via" value={source === "slm" ? "AI model" : "Rule-based parser"} />
        <Row label="Confidence" value={typeof confidence === "number" ? `${Math.round(confidence * 100)}%` : undefined} />
      </div>

      <p className="mt-2 text-xs text-slate-500">
        Nothing has been submitted yet. Review the details above before confirming.
      </p>

      <div className="mt-3 flex gap-2">
        <button
          onClick={onConfirm}
          disabled={loading}
          className="flex flex-1 items-center justify-center gap-1.5 rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-emerald-700 disabled:opacity-60"
        >
          {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <CheckCircle2 className="h-4 w-4" />}
          Confirm &amp; Submit
        </button>
        <button
          onClick={onCancel}
          disabled={loading}
          className="flex items-center justify-center gap-1.5 rounded-lg bg-slate-200 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-300 disabled:opacity-60"
        >
          <XCircle className="h-4 w-4" />
          Cancel
        </button>
      </div>
    </div>
  );
}

function BotOtpGate({
  maskedEmail,
  otpValue,
  otpError,
  isVerifying,
  onOtpChange,
  onVerify,
  onCancel,
}: {
  maskedEmail: string;
  otpValue: string;
  otpError: string | null;
  isVerifying: boolean;
  onOtpChange: (val: string) => void;
  onVerify: () => void;
  onCancel: () => void;
}) {
  return (
    <div className="ml-9 w-[22rem] max-w-full rounded-2xl border border-orange-200 bg-orange-50/70 p-4 shadow-sm">
      <div className="mb-3 flex items-center gap-1.5 text-sm font-semibold text-orange-900">
        <KeyRound className="h-4 w-4" />
        Verify your identity
      </div>
      <p className="text-sm text-slate-600">
        A 4-digit OTP has been sent to{" "}
        <span className="font-medium text-slate-800">{maskedEmail}</span>.
        Enter it below to authorise the payment.
      </p>

      <div className="mt-4">
        <label className="text-sm">
          <span className="mb-1.5 block font-medium text-slate-700">One-time password</span>
          <input
            type="text"
            inputMode="numeric"
            maxLength={4}
            pattern="\d{4}"
            placeholder="_ _ _ _"
            value={otpValue}
            onChange={(e) => onOtpChange(e.target.value.replace(/\D/g, "").slice(0, 4))}
            className="w-36 rounded-lg border border-slate-200 bg-white px-3 py-2 text-center text-2xl tracking-widest text-slate-900 outline-none ring-orange-500/30 focus:ring"
            autoFocus
          />
        </label>
      </div>

      {otpError && (
        <p className="mt-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{otpError}</p>
      )}

      <div className="mt-4 flex gap-2">
        <button
          onClick={onVerify}
          disabled={isVerifying || otpValue.length !== 4}
          className="flex flex-1 items-center justify-center gap-1.5 rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {isVerifying ? <Loader2 className="h-4 w-4 animate-spin" /> : <CheckCircle2 className="h-4 w-4" />}
          {isVerifying ? "Verifying…" : "Verify & Pay"}
        </button>
        <button
          onClick={onCancel}
          disabled={isVerifying}
          className="flex items-center justify-center gap-1.5 rounded-lg bg-slate-200 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-300 disabled:opacity-60"
        >
          <XCircle className="h-4 w-4" />
          Cancel
        </button>
      </div>
    </div>
  );
}

