"use client";

import React, { useState } from "react";

type Msg = { role: 'user' | 'bot'; text: string };

export default function BotChat() {
  const [text, setText] = useState('');
  const [msgs, setMsgs] = useState<Msg[]>([]);
  const [loading, setLoading] = useState(false);

  async function send() {
    if (!text.trim()) return;
    const userMsg: Msg = { role: 'user', text };
    setMsgs((m) => [...m, userMsg]);
    setText('');
    setLoading(true);
    try {
      const resp = await fetch('/api/bot/nl', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text }),
      });
      const json = await resp.json();
      const botText = JSON.stringify(json.intent, null, 2);
      setMsgs((m) => [...m, { role: 'bot', text: botText }]);
    } catch (err) {
      setMsgs((m) => [...m, { role: 'bot', text: 'Error contacting bot service' }]);
    } finally {
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
