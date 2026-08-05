"use client";

import React, { useState } from "react";
import { formatCurrency } from "@/lib/format";

type Msg = { role: 'user' | 'bot'; text: string };

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
    | 'create_payment'
    | 'retry_payment'
    | 'cancel_payment'
    | 'query_payments'
    | 'check_balance'
    | 'list_accounts'
    | 'payment_status'
    | 'unknown';
  payload: PaymentPayload;
  confidence: number;
  summary?: string;
  requiresConfirmation?: boolean;
  readOnly?: boolean;
  source?: 'slm' | 'rules';
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
  create_payment: 'Create Payment',
  retry_payment: 'Retry Payment',
  cancel_payment: 'Cancel Payment',
  query_payments: 'Query Payments',
  check_balance: 'Check Balance',
  list_accounts: 'List Accounts',
  payment_status: 'Payment Status',
};

function formatAccountLine(a: AccountInfo): string {
  return `${a.accountNumber} (${a.accountHolder}) — ${formatCurrency(a.balance, a.currency)} [${a.status}]`;
}

function formatPaymentLine(p: PaymentInfo): string {
  const base = `Payment #${p.paymentId}: ${formatCurrency(p.amount, p.currency)} from ${p.sourceAccount} to ${p.destinationAccount} — status: ${p.status}`;
  return p.errorMessage ? `${base}\nReason: ${p.errorMessage}` : base;
}

export default function BotChat() {
  const [text, setText] = useState('');
  const [msgs, setMsgs] = useState<Msg[]>([]);
  const [loading, setLoading] = useState(false);
  const [pendingCommand, setPendingCommand] = useState<BotCommand | null>(null);
  const [confirming, setConfirming] = useState(false);

  function addBotMsg(t: string) {
    setMsgs((m) => [...m, { role: 'bot', text: t }]);
  }

  async function send() {
    if (!text.trim()) return;
    const userMsg: Msg = { role: 'user', text };
    setMsgs((m) => [...m, userMsg]);
    setText('');
    setLoading(true);
    setPendingCommand(null);
    try {
      const resp = await fetch('/api/bot/nl', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text }),
      });
      const json = await resp.json();
      const intent: BotCommand | undefined = json.intent;

      if (!intent || intent.type === 'unknown') {
        addBotMsg("Sorry, I couldn't understand that request. Try something like \"Create payment of 5000 INR from ACC-10001 to ACC-10002\" or \"What's the balance of ACC-10001?\".");
        return;
      }

      if (intent.type === 'query_payments') {
        addBotMsg('Please use the Payments page to browse and filter transactions.');
        return;
      }

      // Read-only lookups (balance/status/list) have already been resolved
      // by the bot service — just render the result, no confirmation needed.
      if (intent.readOnly) {
        if (json.error) {
          addBotMsg(`❌ ${json.error}`);
        } else if (intent.type === 'check_balance') {
          addBotMsg(formatAccountLine(json.result as AccountInfo));
        } else if (intent.type === 'list_accounts') {
          const accounts = (json.result as AccountInfo[]) || [];
          addBotMsg(accounts.length ? accounts.map(formatAccountLine).join('\n') : 'No accounts found.');
        } else if (intent.type === 'payment_status') {
          addBotMsg(formatPaymentLine(json.result as PaymentInfo));
        }
        return;
      }

      // Every state-changing action (create/retry/cancel) always requires
      // an explicit confirmation click before it's queued for execution.
      setPendingCommand(intent);
    } catch {
      addBotMsg('Error contacting bot service');
    } finally {
      setLoading(false);
    }
  }

  async function confirmPending() {
    if (!pendingCommand) return;
    setConfirming(true);
    try {
      const endpoint = pendingCommand.requiresConfirmation ? '/api/bot/confirm' : '/api/bot/execute';
      const body = pendingCommand.requiresConfirmation
        ? { command: pendingCommand, approver: 'ui-user' }
        : { command: pendingCommand };

      const resp = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      const json = await resp.json();
      if (resp.ok) {
        addBotMsg(`✅ Queued: ${TYPE_LABELS[pendingCommand.type] ?? pendingCommand.type} — it will be processed shortly.`);
      } else {
        addBotMsg(`❌ Failed to queue: ${json.error || json.message || 'unknown error'}`);
      }
    } catch {
      addBotMsg('❌ Error submitting command');
    } finally {
      setPendingCommand(null);
      setConfirming(false);
    }
  }

  function cancelPending() {
    setPendingCommand(null);
    addBotMsg('Cancelled — nothing was submitted.');
  }

  return (
    <div className="p-4 max-w-2xl">
      <div className="border rounded p-3 mb-3 h-64 overflow-auto bg-white">
        {msgs.length === 0 && <div className="text-sm text-slate-500">Ask the bot to create or manage payments.</div>}
        {msgs.map((m, i) => (
          <div key={i} className={`mb-2 ${m.role === 'user' ? 'text-right' : 'text-left'}`}>
            <div className={`inline-block p-2 rounded ${m.role === 'user' ? 'bg-indigo-100' : 'bg-slate-100'}`}>
              <pre className="whitespace-pre-wrap text-sm m-0">{m.text}</pre>
            </div>
          </div>
        ))}
      </div>

      {pendingCommand && (
        <TransactionPreview
          command={pendingCommand}
          loading={confirming}
          onConfirm={confirmPending}
          onCancel={cancelPending}
        />
      )}

      <div className="flex gap-2">
        <input
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && !loading && send()}
          className="flex-1 border rounded p-2"
          placeholder="e.g. Create payment of 50000 INR from account 123 to 456, or check balance of ACC-10001"
        />
        <button onClick={send} disabled={loading} className="bg-indigo-600 text-white px-4 rounded">
          {loading ? '...' : 'Send'}
        </button>
      </div>
    </div>
  );
}

function Row({ label, value }: { label: string; value?: React.ReactNode }) {
  if (value === undefined || value === null || value === '') return null;
  return (
    <div className="flex justify-between py-1 text-sm">
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
    payload.amount !== undefined ? formatCurrency(payload.amount, payload.currency || 'INR') : undefined;

  return (
    <div className="mb-3 rounded-lg border border-indigo-200 bg-indigo-50/60 p-4">
      <div className="mb-2 flex items-center justify-between">
        <span className="text-sm font-semibold text-indigo-900">
          Review {TYPE_LABELS[command.type] ?? command.type}
        </span>
        {requiresConfirmation && (
          <span className="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800">
            High value — approval required
          </span>
        )}
      </div>

      <div className="divide-y divide-indigo-100 rounded-md bg-white/70 px-3">
        <Row label="Amount" value={amountDisplay} />
        <Row label="From account" value={payload.sourceAccount} />
        <Row label="To account" value={payload.destinationAccount} />
        <Row label="Payment ID" value={payload.paymentId} />
        <Row label="Parsed via" value={source === 'slm' ? 'AI model' : 'Rule-based parser'} />
        <Row label="Confidence" value={typeof confidence === 'number' ? `${Math.round(confidence * 100)}%` : undefined} />
      </div>

      <p className="mt-2 text-xs text-slate-500">
        Nothing has been submitted yet. Review the details above before confirming.
      </p>

      <div className="mt-3 flex gap-2">
        <button
          onClick={onConfirm}
          disabled={loading}
          className="rounded bg-emerald-600 px-4 py-2 text-sm font-medium text-white disabled:opacity-60"
        >
          {loading ? 'Submitting…' : 'Confirm & Submit'}
        </button>
        <button
          onClick={onCancel}
          disabled={loading}
          className="rounded bg-slate-200 px-4 py-2 text-sm font-medium text-slate-700 disabled:opacity-60"
        >
          Cancel
        </button>
      </div>
    </div>
  );
}

