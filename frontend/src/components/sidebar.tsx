"use client";

import Link from "next/link";
import Image from "next/image";
import { useState } from "react";
import { usePathname } from "next/navigation";
import { AnimatePresence, motion } from "framer-motion";
import {
  Menu,
  X,
  Home as HomeIcon,
  LayoutDashboard,
  Wallet,
  ArrowLeftRight,
  ClipboardCheck,
  Bot,
  ShieldAlert,
  Activity,
  Inbox,
  type LucideIcon,
} from "lucide-react";
import { useUserStore } from "@/lib/user-store";
import { UserProfile } from "./user-profile";

const ALL_NAV_LINKS: {
  href: string;
  label: string;
  icon: LucideIcon;
  adminOnly: boolean;
  memberHidden: boolean;
}[] = [
  { href: "/", label: "Home", icon: HomeIcon, adminOnly: false, memberHidden: true },
  { href: "/admin", label: "Admin", icon: LayoutDashboard, adminOnly: true, memberHidden: false },
  { href: "/accounts", label: "Accounts", icon: Wallet, adminOnly: false, memberHidden: false },
  { href: "/payments", label: "Payments", icon: ArrowLeftRight, adminOnly: false, memberHidden: true },
  { href: "/admin/review", label: "Admin Review", icon: ClipboardCheck, adminOnly: true, memberHidden: false },
  { href: "/bot", label: "Assistant", icon: Bot, adminOnly: false, memberHidden: false },
  { href: "/rules", label: "Fraud Rules", icon: ShieldAlert, adminOnly: true, memberHidden: false },
  { href: "/events", label: "Events", icon: Activity, adminOnly: true, memberHidden: false },
  { href: "/dlq", label: "DLQ", icon: Inbox, adminOnly: true, memberHidden: false },
];

const navListVariants = {
  hidden: {},
  show: {
    transition: { staggerChildren: 0.045, delayChildren: 0.05 },
  },
};

const navItemVariants = {
  hidden: { opacity: 0, x: -10 },
  show: { opacity: 1, x: 0 },
};

function isLinkActive(pathname: string, href: string): boolean {
  if (href === "/") return pathname === "/";
  return pathname === href || pathname.startsWith(`${href}/`);
}

export function Sidebar() {
  const [open, setOpen] = useState(false);
  const { currentUser } = useUserStore();
  const pathname = usePathname();

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
        className="fixed top-4 left-4 z-40 inline-flex items-center justify-center rounded-lg border border-slate-200 bg-white p-2 text-slate-600 shadow-sm transition hover:bg-slate-50 md:hidden"
        aria-label="Toggle sidebar"
      >
        <AnimatePresence mode="wait" initial={false}>
          <motion.span
            key={open ? "close" : "open"}
            initial={{ rotate: -90, opacity: 0 }}
            animate={{ rotate: 0, opacity: 1 }}
            exit={{ rotate: 90, opacity: 0 }}
            transition={{ duration: 0.15 }}
            className="flex"
          >
            {open ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
          </motion.span>
        </AnimatePresence>
      </button>

      {/* Overlay for mobile */}
      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-30 bg-black/40 md:hidden"
            onClick={() => setOpen(false)}
          />
        )}
      </AnimatePresence>

      {/* Sidebar */}
      <aside
        className={`fixed left-0 top-0 z-40 flex h-full w-56 flex-col border-r border-slate-200 bg-white/95 backdrop-blur-sm transition-transform duration-300 ease-out md:translate-x-0 ${
          open ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        {/* Logo */}
        <div className="border-b border-slate-200 px-5 py-5">
          <Link
            href="/"
            className="flex items-center gap-2.5"
            onClick={() => setOpen(false)}
          >
            <motion.span
              whileHover={{ rotate: -8, scale: 1.08 }}
              transition={{ type: "spring", stiffness: 400, damping: 15 }}
              className="flex"
            >
              <Image
                src="/rupeex-logo.svg"
                alt="RupeeX Logo"
                width={36}
                height={36}
                className="rounded-xl shadow-sm"
              />
            </motion.span>
            <div>
              <span className="block text-base font-bold tracking-tight text-slate-900 leading-tight">
                RupeeX
              </span>
              <span className="block text-[11px] font-medium text-orange-600 tracking-wide">
                Ops Console
              </span>
            </div>
          </Link>
        </div>

        {/* Navigation Links */}
        <motion.nav
          variants={navListVariants}
          initial="hidden"
          animate="show"
          className="flex-1 overflow-y-auto px-3 py-5 space-y-1"
        >
          {visibleLinks.map((link) => {
            const active = isLinkActive(pathname, link.href);
            const Icon = link.icon;
            return (
              <motion.div
                key={link.href}
                variants={navItemVariants}
                whileHover={{ x: 4 }}
                whileTap={{ scale: 0.97 }}
                transition={{ type: "spring", stiffness: 420, damping: 26 }}
              >
                <Link
                  href={link.href}
                  onClick={() => setOpen(false)}
                  className={`relative flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors ${
                    active
                      ? "text-orange-700"
                      : "text-slate-600 hover:text-orange-600"
                  }`}
                >
                  {active && (
                    <motion.span
                      layoutId="sidebar-active-pill"
                      className="absolute inset-0 -z-10 rounded-lg bg-orange-500/10 ring-1 ring-orange-500/20"
                      transition={{ type: "spring", stiffness: 380, damping: 32 }}
                    />
                  )}
                  <Icon
                    className={`h-4 w-4 shrink-0 ${
                      active ? "text-orange-600" : "text-slate-400"
                    }`}
                  />
                  {link.label}
                </Link>
              </motion.div>
            );
          })}
        </motion.nav>

        {/* Footer - User Profile */}
        <div className="border-t border-slate-200 px-4 py-4">
          <UserProfile />
        </div>
      </aside>
    </>
  );
}

