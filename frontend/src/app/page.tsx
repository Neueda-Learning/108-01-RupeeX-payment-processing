import Link from "next/link";
import {
  ArrowRight,
  Building2,
  CircleDollarSign,
  UserRoundSearch,
} from "lucide-react";
import { getDashboardStats, getPayments } from "@/lib/api";
import { formatCurrency, formatPercent } from "@/lib/format";
import { MemberRedirect } from "@/components/member-redirect";

const VIEWS = [
  {
    href: "/admin",
    title: "Admin View",
    subtitle: "Operations control room",
    description:
      "See all payments, status distribution, and platform throughput in one place.",
    icon: Building2,
    accent: "from-orange-500/25 to-amber-400/10",
  },
  {
    href: "/source",
    title: "Source Account View",
    subtitle: "Outgoing payment perspective",
    description:
      "Filter by source account and monitor funds sent, payment count, and success trend.",
    icon: CircleDollarSign,
    accent: "from-blue-500/25 to-cyan-400/10",
  },
  {
    href: "/destination",
    title: "Destination Account View",
    subtitle: "Incoming payment perspective",
    description:
      "Filter by destination account and monitor settlement flow and incoming value.",
    icon: UserRoundSearch,
    accent: "from-emerald-500/25 to-lime-400/10",
  },
];

export default async function Home() {
  let paymentsCount = 0;
  let totalVolume = 0;
  let successRate = 0;
  let loadError: string | null = null;

  try {
    const [payments, stats] = await Promise.all([
      getPayments(),
      getDashboardStats(),
    ]);
    paymentsCount = payments.length;
    totalVolume = stats.totalVolume;
    successRate = stats.successRate;
  } catch (error) {
    loadError =
      error instanceof Error
        ? error.message
        : "Unable to load overview metrics";
  }

  return (
    <div className="mx-auto max-w-7xl px-6 py-10 lg:px-8">
      <MemberRedirect to="/accounts" />
      <section className="panel relative overflow-hidden rounded-3xl p-8">
        <div className="absolute -right-10 -top-16 h-52 w-52 rounded-full bg-orange-500/10 blur-2xl" />
        <div className="absolute -left-14 bottom-0 h-44 w-44 rounded-full bg-blue-500/10 blur-2xl" />
        <p className="text-xs font-semibold uppercase tracking-[0.2em] text-orange-700">
          Payment Operations Console
        </p>
        <h1 className="mt-2 max-w-3xl text-4xl font-semibold leading-tight text-slate-900">
          Choose the right view for the right operator.
        </h1>
        <p className="mt-3 max-w-2xl text-slate-600">
          No generic dashboard noise. Open the role-specific workspace you need
          and work directly on real payment data.
        </p>

        <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
          <div className="rounded-2xl border border-black/10 bg-white/70 p-4">
            <p className="text-xs uppercase tracking-wide text-slate-500">
              Total payment volume
            </p>
            <p className="mt-1 text-2xl font-semibold text-slate-900">
              {loadError ? "Unavailable" : formatCurrency(totalVolume, "INR")}
            </p>
          </div>
          <div className="rounded-2xl border border-black/10 bg-white/70 p-4">
            <p className="text-xs uppercase tracking-wide text-slate-500">
              Payments loaded
            </p>
            <p className="mt-1 text-2xl font-semibold text-slate-900">
              {loadError
                ? "Unavailable"
                : paymentsCount.toLocaleString("en-IN")}
            </p>
          </div>
          <div className="rounded-2xl border border-black/10 bg-white/70 p-4">
            <p className="text-xs uppercase tracking-wide text-slate-500">
              Success rate
            </p>
            <p className="mt-1 text-2xl font-semibold text-slate-900">
              {loadError ? "Unavailable" : formatPercent(successRate)}
            </p>
          </div>
        </div>

        {loadError && (
          <p className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {loadError}
          </p>
        )}
      </section>

      <section className="mt-8 grid grid-cols-1 gap-5 lg:grid-cols-3">
        {VIEWS.map((view) => {
          const Icon = view.icon;
          return (
            <Link
              key={view.href}
              href={view.href}
              className="panel group relative overflow-hidden rounded-2xl p-5 transition hover:-translate-y-0.5"
            >
              <div
                className={`pointer-events-none absolute inset-0 bg-gradient-to-br ${view.accent}`}
              />
              <div className="relative">
                <div className="inline-flex rounded-xl border border-black/10 bg-white/70 p-2">
                  <Icon className="h-5 w-5 text-slate-700" />
                </div>
                <p className="mt-4 text-xs uppercase tracking-wide text-slate-500">
                  {view.subtitle}
                </p>
                <h2 className="mt-1 text-xl font-semibold text-slate-900">
                  {view.title}
                </h2>
                <p className="mt-2 text-sm text-slate-600">
                  {view.description}
                </p>
                <span className="mt-5 inline-flex items-center gap-1 text-sm font-medium text-orange-700 group-hover:text-orange-800">
                  Open view
                  <ArrowRight className="h-4 w-4" />
                </span>
              </div>
            </Link>
          );
        })}
      </section>

      <section className="panel mt-8 rounded-2xl p-5">
        <h3 className="text-lg font-semibold text-slate-900">
          Need direct actions?
        </h3>
        <p className="mt-1 text-sm text-slate-600">
          Use Payments to create transactions, Events to monitor live updates,
          Fraud Rules to tune policy, and DLQ for recovery actions.
        </p>
        <div className="mt-4 flex flex-wrap gap-3">
          {[
            { href: "/payments", label: "Open Payments" },
            { href: "/events", label: "Open Events" },
            { href: "/rules", label: "Open Fraud Rules" },
            { href: "/dlq", label: "Open DLQ" },
          ].map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className="rounded-lg border border-black/10 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:border-orange-300 hover:text-orange-700"
            >
              {item.label}
            </Link>
          ))}
        </div>
      </section>
    </div>
  );
}
