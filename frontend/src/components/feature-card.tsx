import type { LucideIcon } from "lucide-react";

interface FeatureCardProps {
  title: string;
  description: string;
  icon: LucideIcon;
}

export function FeatureCard({ title, description, icon: Icon }: FeatureCardProps) {
  return (
    <div className="group rounded-2xl border border-black/5 bg-white p-6 transition hover:-translate-y-0.5 hover:shadow-md dark:border-white/10 dark:bg-slate-900">
      <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-emerald-600/10 text-emerald-600 transition group-hover:bg-emerald-600 group-hover:text-white dark:text-emerald-400">
        <Icon className="h-5 w-5" />
      </span>
      <h3 className="mt-4 text-base font-semibold text-slate-900 dark:text-white">
        {title}
      </h3>
      <p className="mt-2 text-sm leading-relaxed text-slate-500 dark:text-slate-400">
        {description}
      </p>
    </div>
  );
}
