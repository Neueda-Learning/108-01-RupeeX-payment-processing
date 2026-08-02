import {
  Landmark,
  ReceiptText,
  ShieldCheck,
  TrendingUp,
  Lock,
  RadioTower,
  History,
  RefreshCcw,
} from "lucide-react";

import { Hero } from "@/components/hero";
import { Section } from "@/components/section";
import { StatCard } from "@/components/stat-card";
import { FeatureCard } from "@/components/feature-card";
import { PaymentsTable } from "@/components/payments-table";
import { AccountsGrid } from "@/components/accounts-grid";
import { getAccounts, getDashboardStats, getPayments } from "@/lib/api";
import { formatCurrency, formatPercent } from "@/lib/format";

const FEATURES = [
  {
    title: "Secure by design",
    description:
      "Idempotency keys, validation, and audited state transitions prevent duplicate or inconsistent payments.",
    icon: Lock,
  },
  {
    title: "Real-time status tracking",
    description:
      "Every payment moves through a well-defined lifecycle — pending, processing, completed, or failed.",
    icon: RadioTower,
  },
  {
    title: "Full audit trail",
    description:
      "Every status change is recorded with a timestamp, actor, and remarks for compliance and debugging.",
    icon: History,
  },
  {
    title: "Automatic reconciliation",
    description:
      "Built to integrate with retry and reconciliation workflows for failed or stalled transactions.",
    icon: RefreshCcw,
  },
];

export default async function Home() {
  const [payments, accounts, stats] = await Promise.all([
    getPayments(),
    getAccounts(),
    getDashboardStats(),
  ]);

  return (
    <>
      <Hero />

      <Section
        id="dashboard"
        eyebrow="Overview"
        title="Live payment metrics"
        description="A snapshot of processing volume, reliability, and account activity."
      >
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <StatCard
            label="Total volume"
            value={formatCurrency(stats.totalVolume, "INR")}
            icon={TrendingUp}
            hint="Last 30 days"
          />
          <StatCard
            label="Payments processed"
            value={stats.totalPayments.toLocaleString("en-IN")}
            icon={ReceiptText}
            hint="Across all accounts"
          />
          <StatCard
            label="Success rate"
            value={formatPercent(stats.successRate)}
            icon={ShieldCheck}
            hint="Completed vs. failed"
          />
          <StatCard
            label="Active accounts"
            value={stats.activeAccounts.toString()}
            icon={Landmark}
            hint="Currently enabled"
          />
        </div>
      </Section>

      <Section
        id="payments"
        eyebrow="Transactions"
        title="Recent payments"
        description="The latest payments moving through the processing pipeline."
      >
        <PaymentsTable payments={payments} />
      </Section>

      <Section
        id="accounts"
        eyebrow="Accounts"
        title="Connected accounts"
        description="Source and destination accounts registered with RupeeX."
      >
        <AccountsGrid accounts={accounts} />
      </Section>

      <Section
        id="features"
        eyebrow="Why RupeeX"
        title="Built for enterprise payment operations"
        description="A robust foundation for teams that need reliability, traceability, and control."
      >
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {FEATURES.map((feature) => (
            <FeatureCard key={feature.title} {...feature} />
          ))}
        </div>
      </Section>
    </>
  );
}
