"use client";

import React, { useState } from "react";

type Msg = { role: 'user' | 'bot'; text: string };

export default function BotChat() {
  const [text, setText] = useState('');
  const [msgs, setMsgs] = useState<Msg[]>([]);
  const [loading, setLoading] = useState(false);
  const [pendingCommand, setPendingCommand] = useState<any>(null);

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
      const intent = json.intent;

      if (!intent || intent.type === 'unknown') {
        addBotMsg("Sorry, I couldn't understand that request.");
        return;
      }

      addBotMsg(intent.summary || JSON.stringify(intent, null, 2));

      if (intent.requiresConfirmation) {
        setPendingCommand(intent);
        addBotMsg('This is a high-value payment and requires confirmation. Click "Confirm" below to proceed.');
        return;
      }

      await executeCommand(intent);
    } catch (err) {
      addBotMsg('Error contacting bot service');
    } finally {
      setLoading(false);
    }
  }

  async function executeCommand(command: any) {
    try {
      const resp = await fetch('/api/bot/execute', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ command }),
      });
      const json = await resp.json();
      if (resp.ok) {
        addBotMsg(`Queued: ${command.type} — it will be processed shortly.`);
      } else {
        addBotMsg(`Failed to queue command: ${json.error || 'unknown error'}`);
      }
    } catch (err) {
      addBotMsg('Error queuing command');
    }
  }

  async function confirmPending() {
    if (!pendingCommand) return;
    setLoading(true);
    try {
      const resp = await fetch('/api/bot/confirm', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ command: pendingCommand, approver: 'ui-user' }),
      });
      const json = await resp.json();
      if (resp.ok) {
        addBotMsg(`Confirmed and queued: ${pendingCommand.type}.`);
      } else {
        addBotMsg(`Failed to confirm: ${json.error || 'unknown error'}`);
      }
    } catch (err) {
      addBotMsg('Error confirming command');
    } finally {
      setPendingCommand(null);
      setLoading(false);
    }
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
        <div className="mb-3 flex gap-2">
          <button onClick={confirmPending} disabled={loading} className="bg-emerald-600 text-white px-4 py-2 rounded">
            Confirm
          </button>
          <button onClick={() => setPendingCommand(null)} disabled={loading} className="bg-slate-200 px-4 py-2 rounded">
            Cancel
          </button>
        </div>
      )}

      <div className="flex gap-2">
        <input
          value={text}
          onChange={(e) => setText(e.target.value)}
          className="flex-1 border rounded p-2"
          placeholder="e.g. Create payment of 50000 INR from account 123 to 456"
        />
        <button onClick={send} disabled={loading} className="bg-indigo-600 text-white px-4 rounded">
          {loading ? '...' : 'Send'}
        </button>
      </div>
    </div>
  );
}

