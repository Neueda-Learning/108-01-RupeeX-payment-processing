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
  { href: "/admin/review", label: "Admin Review", adminOnly: true, memberHidden: false },
  { href: "/bot", label: "Assistant", adminOnly: false, memberHidden: false },
  { href: "/rules", label: "Fraud Rules", adminOnly: true, memberHidden: false },
  { href: "/events", label: "Events", adminOnly: true, memberHidden: false },
  { href: "/dlq", label: "DLQ", adminOnly: true, memberHidden: false },
];

export function Sidebar() {
  const [open, setOpen] = useState(false);
  const { currentUser } = useUserStore();

  const visibleLinks = ALL_NAV_LINKS.filter((link) => {
    if (link.adminOnly && currentUser && currentUser.role !== "admin") return false;
    if (link.memberHidden && currentUser?.role === "member") return false;
    return true;
  });

  return (
    <>
      {/* Mobile Toggle Button */}
      <button
        onClick={() => setOpen(!open)}
        className="fixed top-4 left-4 z-40 inline-flex items-center justify-center rounded-lg border border-slate-200 bg-white p-2 text-slate-600 md:hidden hover:bg-slate-50"
        aria-label="Toggle sidebar"
      >
        {open ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
      </button>

      {/* Overlay for mobile */}
      {open && (
        <div
          className="fixed inset-0 z-30 bg-black/40 md:hidden"
          onClick={() => setOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside
        className={`fixed left-0 top-0 z-40 flex h-full w-64 flex-col border-r border-slate-200 bg-white transition-transform duration-300 md:translate-x-0 ${
          open ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        {/* Logo */}
        <div className="border-b border-slate-200 px-6 py-6">
          <Link href="/" className="flex items-center gap-3 font-semibold" onClick={() => setOpen(false)}>
            <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-orange-500 text-lg font-bold text-white shadow-sm shadow-orange-500/30">
              ₹
            </span>
            <span className="text-lg tracking-tight text-slate-900">
              RupeeX
            </span>
          </Link>
        </div>

        {/* Navigation Links */}
        <nav className="flex-1 overflow-y-auto px-4 py-6 space-y-2">
          {visibleLinks.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              onClick={() => setOpen(false)}
              className="flex items-center gap-3 rounded-lg px-4 py-2.5 text-sm font-medium text-slate-600 transition hover:bg-orange-50 hover:text-orange-700"
            >
              {link.label}
            </Link>
          ))}
        </nav>

        {/* Footer - User Profile */}
        <div className="border-t border-slate-200 px-4 py-4">
          <UserProfile />
        </div>
      </aside>
    </>
  );
}

