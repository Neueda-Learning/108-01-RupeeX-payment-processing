"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { MessageCircle, X } from "lucide-react";
import BotChat from "@/components/bot-chat";

/**
 * Global bottom-right floating chat launcher. Mounted once in the root
 * layout so it's available on every page. Shares the same core BotChat
 * component as the full-screen /bot page; the expand button there
 * navigates to that page instead of duplicating the UI.
 */
export function BotChatWidget() {
  const [open, setOpen] = useState(false);
  const router = useRouter();

  return (
    <>
      {open && (
        <div className="fixed bottom-24 right-4 z-50 h-[32rem] w-[24rem] max-w-[calc(100vw-2rem)] sm:right-6">
          <div className="relative h-full w-full">
            <button
              onClick={() => setOpen(false)}
              className="absolute -top-3 -right-3 z-10 flex h-8 w-8 items-center justify-center rounded-full bg-slate-900 text-white shadow-lg transition hover:bg-slate-700"
              aria-label="Close chat"
            >
              <X className="h-4 w-4" />
            </button>
            <BotChat variant="popup" onExpand={() => router.push("/bot")} />
          </div>
        </div>
      )}

      <button
        onClick={() => setOpen((v) => !v)}
        className="fixed bottom-6 right-4 z-50 flex h-14 w-14 items-center justify-center rounded-full bg-orange-600 text-white shadow-xl transition hover:bg-orange-700 sm:right-6"
        aria-label={open ? "Close RupeeX assistant" : "Open RupeeX assistant"}
        title="RupeeX Assistant"
      >
        {open ? <X className="h-6 w-6" /> : <MessageCircle className="h-6 w-6" />}
      </button>
    </>
  );
}
