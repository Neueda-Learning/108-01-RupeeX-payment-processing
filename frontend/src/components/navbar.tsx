"use client";

import Link from "next/link";
import { useState } from "react";
import { Menu, X } from "lucide-react";
import { useUserStore } from "@/lib/user-store";
import { UserProfile } from "./user-profile";

const ALL_NAV_LINKS = [
  { href: "/", label: "Home", adminOnly: false, memberHidden: true },
  { href: "/admin", label: "Admin", adminOnly: true, memberHidden: false },
  { href: "/accounts", label: "Accounts", adminOnly: false, memberHidden: false },
  { href: "/payments", label: "Payments", adminOnly: false, memberHidden: true },
  { href: "/rules", label: "Fraud Rules", adminOnly: true, memberHidden: false },
  { href: "/events", label: "Events", adminOnly: true, memberHidden: false },
  { href: "/dlq", label: "DLQ", adminOnly: true, memberHidden: false },
];

export function Navbar() {
  const [open, setOpen] = useState(false);
  const { currentUser } = useUserStore();

  const visibleLinks = ALL_NAV_LINKS.filter((link) => {
    // Admin-only links: hidden for logged-in non-admins (but visible when no user selected)
    if (link.adminOnly && currentUser && currentUser.role !== "admin") return false;
    // Member-hidden links: hidden for members
    if (link.memberHidden && currentUser?.role === "member") return false;
    return true;
  });

  return (
    <header className="sticky top-0 z-50 border-b border-black/5 bg-white/80 backdrop-blur-md">
      <nav className="mx-auto flex max-w-7xl items-center justify-between px-6 py-4 lg:px-8">
        <Link href="/" className="flex items-center gap-2 font-semibold">
          <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-orange-500 text-white shadow-sm shadow-orange-500/30">
            ₹
          </span>
          <span className="text-lg tracking-tight text-slate-900">
            RupeeX Ops
          </span>
        </Link>

        <div className="hidden items-center gap-6 md:flex">
          {visibleLinks.map((link) => (
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
          <UserProfile />
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
            {visibleLinks.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                onClick={() => setOpen(false)}
                className="text-sm font-medium text-slate-600"
              >
                {link.label}
              </Link>
            ))}
            <div className="pt-2 border-t border-slate-100">
              <UserProfile />
            </div>
          </div>
        </div>
      )}
    </header>
  );
}
