"use client";

import Link from "next/link";
import { useState } from "react";
import { Menu, X, ShieldCheck } from "lucide-react";

const NAV_LINKS = [
  { href: "/", label: "Home" },
  { href: "/admin", label: "Admin View" },
  { href: "/source", label: "Source Account View" },
  { href: "/destination", label: "Destination Account View" },
  { href: "/payments", label: "Payments" },
  { href: "/events", label: "Events" },
  { href: "/rules", label: "Fraud Rules" },
  { href: "/dlq", label: "DLQ" },
];

export function Navbar() {
  const [open, setOpen] = useState(false);

  return (
    <header className="sticky top-0 z-50 border-b border-black/5 bg-white/80 backdrop-blur-md">
      <nav className="mx-auto flex max-w-7xl items-center justify-between px-6 py-4 lg:px-8">
        <Link href="#top" className="flex items-center gap-2 font-semibold">
          <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-orange-500 text-white shadow-sm shadow-orange-500/30">
            ₹
          </span>
          <span className="text-lg tracking-tight text-slate-900">
            RupeeX Ops
          </span>
        </Link>

        <div className="hidden items-center gap-6 md:flex">
          {NAV_LINKS.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className="text-sm font-medium text-slate-600 transition hover:text-orange-600"
            >
              {link.label}
            </Link>
          ))}
        </div>

        <div className="hidden items-center gap-3 md:flex">
          <span className="inline-flex items-center gap-1.5 rounded-full bg-orange-500/10 px-3 py-1 text-xs font-medium text-orange-700 ring-1 ring-inset ring-orange-500/20">
            <ShieldCheck className="h-3.5 w-3.5" />
            Role-based operations views
          </span>
        </div>

        <button
          className="inline-flex items-center justify-center rounded-md p-2 text-slate-600 md:hidden"
          onClick={() => setOpen((v) => !v)}
          aria-label="Toggle navigation menu"
        >
          {open ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
        </button>
      </nav>

      {open && (
        <div className="border-t border-black/5 px-6 py-4 md:hidden">
          <div className="flex flex-col gap-4">
            {NAV_LINKS.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                onClick={() => setOpen(false)}
                className="text-sm font-medium text-slate-600"
              >
                {link.label}
              </Link>
            ))}
          </div>
        </div>
      )}
    </header>
  );
}
