"use client";

import { useEffect, useMemo, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
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

export default function EventsPage() {
  const [events, setEvents] = useState<SystemEvent[]>([]);
  const [socketConnected, setSocketConnected] = useState(false);

  useEffect(() => {
    let mounted = true;

    getEvents()
      .then((rows) => {
        if (mounted) {
          setEvents(rows);
        }
      })
      .catch(() => {
        if (mounted) {
          setEvents([]);
        }
      });

    const client = new Client({
      webSocketFactory: () => new SockJS(`${API_BASE_URL}/ws/events`),
      reconnectDelay: 4000,
      debug: () => undefined,
    });

    client.onConnect = () => {
      setSocketConnected(true);
      client.subscribe("/topic/events", (message) => {
        const payload = JSON.parse(message.body) as SystemEvent;
        setEvents((current) => uniqueById([payload, ...current]).slice(0, 100));
      });
    };

    client.onStompError = () => {
      setSocketConnected(false);
    };

    client.onWebSocketClose = () => {
      setSocketConnected(false);
    };

    client.activate();

    const polling = window.setInterval(async () => {
      if (socketConnected) {
        return;
      }
      try {
        const rows = await getEvents();
        setEvents(rows);
      } catch {
        // Keep existing rows when polling fails.
      }
    }, 5000);

    return () => {
      mounted = false;
      window.clearInterval(polling);
      client.deactivate();
    };
  }, [socketConnected]);

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
