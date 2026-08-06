"use client";

import * as Dialog from "@radix-ui/react-dialog";
import { X, Loader2 } from "lucide-react";
import { useState } from "react";
import { createAndApproveUser } from "@/lib/onboarding-api";
import { useUserStore } from "@/lib/user-store";
import type { UserRole } from "@/lib/user-store";

// Countries list based on:
// 1. Supported currencies (INR, USD, GBP, EUR, SGD, AED)
// 2. Countries in seed data (IN, US)
// 3. High-risk countries from fraud rules (KP, IR, SY, CU, ZW, MM, VE, BY)
const COUNTRIES = [
  // Primary countries in seed data
  { code: "IN", name: "India" },
  { code: "US", name: "United States" },

  // Other supported currencies countries
  { code: "GB", name: "United Kingdom" },
  { code: "DE", name: "Germany" },
  { code: "FR", name: "France" },
  { code: "SG", name: "Singapore" },
  { code: "AE", name: "United Arab Emirates" },

  // High-risk countries (from fraud rules)
  { code: "KP", name: "North Korea" },
  { code: "IR", name: "Iran" },
  { code: "SY", name: "Syria" },
  { code: "CU", name: "Cuba" },
  { code: "ZW", name: "Zimbabwe" },
  { code: "MM", name: "Myanmar" },
  { code: "VE", name: "Venezuela" },
  { code: "BY", name: "Belarus" },

  // Other common countries for international payments
  { code: "CA", name: "Canada" },
  { code: "AU", name: "Australia" },
  { code: "NZ", name: "New Zealand" },
  { code: "JP", name: "Japan" },
  { code: "CN", name: "China" },
  { code: "HK", name: "Hong Kong" },
  { code: "MY", name: "Malaysia" },
  { code: "TH", name: "Thailand" },
  { code: "PH", name: "Philippines" },
  { code: "VN", name: "Vietnam" },
  { code: "KR", name: "South Korea" },
  { code: "TW", name: "Taiwan" },
  { code: "SA", name: "Saudi Arabia" },
  { code: "QA", name: "Qatar" },
  { code: "KW", name: "Kuwait" },
  { code: "BR", name: "Brazil" },
  { code: "MX", name: "Mexico" },
  { code: "ZA", name: "South Africa" },
];

interface AddUserModalProps {
  open: boolean;
  onClose: () => void;
}

export function AddUserModal({ open, onClose }: AddUserModalProps) {
  const addUser = useUserStore((s) => s.addUser);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [form, setForm] = useState({
    fullName: "",
    email: "",
    phone: "",
    dob: "",
    accountType: "SAVINGS",
    currency: "INR",
    countryCode: "IN",
    role: "member" as UserRole,
  });

  function handleChange(e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const customer = await createAndApproveUser({
        ...form,
        dob: form.dob || undefined,
        countryCode: form.countryCode || undefined,
      });
      addUser({
        customerId: customer.customerId,
        name: form.fullName,
        email: form.email,
        phone: form.phone,
        accountNumber: customer.accountNumber,
        role: form.role,
      });
      onClose();
      setForm({
        fullName: "",
        email: "",
        phone: "",
        dob: "",
        accountType: "SAVINGS",
        currency: "INR",
        countryCode: "IN",
        role: "member",
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to create user");
    } finally {
      setLoading(false);
    }
  }

  return (
    <Dialog.Root open={open} onOpenChange={(o) => !o && onClose()}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-50 bg-black/40 backdrop-blur-sm" />
        <Dialog.Content className="fixed left-1/2 top-1/2 z-50 w-full max-w-md -translate-x-1/2 -translate-y-1/2 rounded-2xl bg-white p-6 shadow-xl">
          <div className="flex items-center justify-between">
            <Dialog.Title className="text-lg font-semibold text-slate-900">
              Add New User
            </Dialog.Title>
            <button onClick={onClose} className="rounded-md p-1 text-slate-400 hover:text-slate-600">
              <X className="h-5 w-5" />
            </button>
          </div>

          <form onSubmit={handleSubmit} className="mt-5 space-y-4">
            {/* Personal details */}
            <div className="grid grid-cols-2 gap-3">
              <div className="col-span-2">
                <label className="block text-xs font-medium text-slate-600">Full Name *</label>
                <input name="fullName" value={form.fullName} onChange={handleChange} required
                  className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-orange-400 focus:outline-none" />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-600">Email *</label>
                <input name="email" type="email" value={form.email} onChange={handleChange} required
                  className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-orange-400 focus:outline-none" />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-600">Phone *</label>
                <input name="phone" value={form.phone} onChange={handleChange} required
                  className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-orange-400 focus:outline-none" />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-600">Date of Birth</label>
                <input name="dob" type="date" value={form.dob} onChange={handleChange}
                  className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-orange-400 focus:outline-none" />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-600">Role *</label>
                <select name="role" value={form.role} onChange={handleChange} required
                  className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-orange-400 focus:outline-none">
                  <option value="admin">Admin</option>
                  <option value="member">Member</option>
                </select>
              </div>
            </div>

            <hr className="border-slate-100" />
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Account Details</p>

            <div className="grid grid-cols-2 gap-3">
              <div className="col-span-2">
                <label className="block text-xs font-medium text-slate-600">Account Number</label>
                <div className="mt-1 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-400 cursor-not-allowed select-none tracking-widest">
                  RUPX••••••
                </div>
                <p className="mt-1 text-xs text-slate-400">Auto-generated on account creation</p>
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-600">Account Type *</label>
                <select name="accountType" value={form.accountType} onChange={handleChange}
                  className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-orange-400 focus:outline-none">
                  <option value="SAVINGS">Savings</option>
                  <option value="CURRENT">Current</option>
                </select>
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-600">Currency *</label>
                <select name="currency" value={form.currency} onChange={handleChange}
                  className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-orange-400 focus:outline-none">
                  <option value="INR">INR</option>
                  <option value="USD">USD</option>
                  <option value="GBP">GBP</option>
                  <option value="EUR">EUR</option>
                </select>
              </div>
               <div className="col-span-2">
                 <label className="block text-xs font-medium text-slate-600">Country *</label>
                 <select name="countryCode" value={form.countryCode} onChange={handleChange} required
                   className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-orange-400 focus:outline-none">
                   <option value="">Select a country...</option>
                   {COUNTRIES.map((country) => (
                     <option key={country.code} value={country.code}>
                       {country.name} ({country.code})
                     </option>
                   ))}
                 </select>
               </div>
            </div>

            {error && (
              <p className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700">
                {error}
              </p>
            )}

            <div className="flex justify-end gap-2 pt-1">
              <button type="button" onClick={onClose}
                className="rounded-lg border border-slate-200 px-4 py-2 text-sm text-slate-600 hover:bg-slate-50">
                Cancel
              </button>
              <button type="submit" disabled={loading}
                className="inline-flex items-center gap-2 rounded-lg bg-orange-500 px-4 py-2 text-sm font-medium text-white hover:bg-orange-600 disabled:opacity-60">
                {loading && <Loader2 className="h-4 w-4 animate-spin" />}
                {loading ? "Creating..." : "Add User"}
              </button>
            </div>
          </form>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}

