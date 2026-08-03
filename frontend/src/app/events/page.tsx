"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { API_BASE_URL, getEvents } from "@/lib/api";
import type { SystemEvent } from "@/lib/types";
import { formatDateTime } from "@/lib/format";

function uniqueById(events: SystemEvent[]): SystemEvent[] {
  const seen = new Set<number>();
  const output: SystemEvent[] = [];
  for (const event of events) {
    if (!seen.has(event.id)) {
      seen.add(event.id);
      output.push(event);
    }
  }
  return output;
}

/** Convert http(s)://host to ws(s)://host for native WebSocket */
function toWsUrl(base: string): string {
  return base.replace(/^http/, "ws") + "/ws/events/websocket";
}

export default function EventsPage() {
  const [events, setEvents] = useState<SystemEvent[]>([]);
  const [socketConnected, setSocketConnected] = useState(false);
  const wsRef = useRef<WebSocket | null>(null);
  const stompSessionRef = useRef<string | null>(null);

  useEffect(() => {
    let mounted = true;

    // Load existing events from REST on mount
    getEvents()
      .then((rows) => {
        if (mounted) setEvents(rows);
      })
      .catch(() => {
        if (mounted) setEvents([]);
      });

    // Use native WebSocket with STOMP framing to avoid SockJS /info polling
    const wsUrl = toWsUrl(API_BASE_URL);

    function connect() {
      const ws = new WebSocket(wsUrl);
      wsRef.current = ws;

      ws.onopen = () => {
        // STOMP CONNECT frame
        ws.send("CONNECT\naccept-version:1.2\nheart-beat:0,0\n\n\0");
      };

      ws.onmessage = (e: MessageEvent<string>) => {
        const frame = String(e.data);

        if (frame.startsWith("CONNECTED")) {
          if (mounted) setSocketConnected(true);
          stompSessionRef.current = frame;
          // Subscribe to /topic/events
          ws.send("SUBSCRIBE\nid:sub-0\ndestination:/topic/events\n\n\0");
          return;
        }

        if (frame.startsWith("MESSAGE")) {
          const bodyStart = frame.indexOf("\n\n");
          if (bodyStart === -1) return;
          const body = frame.slice(bodyStart + 2).replace(/\0$/, "");
          try {
            const payload = JSON.parse(body) as SystemEvent;
            setEvents((prev) => uniqueById([payload, ...prev]).slice(0, 100));
          } catch {
            /* ignore malformed frames */
          }
        }
      };

      ws.onclose = () => {
        if (mounted) {
          setSocketConnected(false);
          // Reconnect after 5s
          setTimeout(() => {
            if (mounted) connect();
          }, 5000);
        }
      };

      ws.onerror = () => {
        ws.close();
      };
    }

    connect();

    // Polling fallback: refresh from REST every 10s when WS is down
    const poll = window.setInterval(async () => {
      if (socketConnected) return;
      try {
        const rows = await getEvents();
        if (mounted) setEvents(rows);
      } catch {
        /* keep existing rows */
      }
    }, 10000);

    return () => {
      mounted = false;
      window.clearInterval(poll);
      wsRef.current?.close();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const connectionBadge = useMemo(
    () =>
      socketConnected
        ? "bg-emerald-500/10 text-emerald-700"
        : "bg-amber-500/10 text-amber-700",
    [socketConnected],
  );

  return (
    <div className="mx-auto max-w-6xl space-y-6 px-6 py-10 lg:px-8">
      <header className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-sm font-semibold uppercase tracking-wide text-orange-700">
            Live Event Feed
          </p>
          <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-900">
            Operational event stream
          </h1>
          <p className="text-slate-600">
            Real-time updates for payment progress, retries, failures, and risk
            alerts.
          </p>
        </div>
        <span
          className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${connectionBadge}`}
        >
          {socketConnected ? "WebSocket connected" : "Polling fallback"}
        </span>
      </header>

      <section className="panel rounded-2xl overflow-hidden">
        <div className="divide-y divide-black/5">
          {events.length === 0 ? (
            <div className="px-6 py-8 text-sm text-slate-500">
              No events yet.
            </div>
          ) : (
            events.map((event) => (
              <article key={event.id} className="px-6 py-4">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="rounded-md bg-slate-100 px-2 py-1 text-xs font-semibold text-slate-700">
                    {event.eventType}
                  </span>
                  <span className="text-xs text-slate-500">
                    {formatDateTime(event.createdAt)}
                  </span>
                  {typeof event.entityId === "number" && (
                    <span className="text-xs text-slate-500">
                      Payment #{event.entityId}
                    </span>
                  )}
                </div>
                <pre className="mt-2 overflow-x-auto whitespace-pre-wrap rounded-lg bg-slate-50 p-3 text-xs text-slate-700">
                  {event.payload}
                </pre>
              </article>
            ))
          )}
        </div>
      </section>
    </div>
  );
}
