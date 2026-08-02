import type { ReactNode } from "react";

interface SectionProps {
  id?: string;
  eyebrow?: string;
  title: string;
  description?: string;
  action?: ReactNode;
  children: ReactNode;
}

export function Section({
  id,
  eyebrow,
  title,
  description,
  action,
  children,
}: SectionProps) {
  return (
    <section id={id} className="mx-auto max-w-7xl px-6 py-16 lg:px-8">
      <div className="flex flex-col items-start justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          {eyebrow && (
            <p className="text-xs font-semibold uppercase tracking-wider text-emerald-600 dark:text-emerald-400">
              {eyebrow}
            </p>
          )}
          <h2 className="mt-1 text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl dark:text-white">
            {title}
          </h2>
          {description && (
            <p className="mt-2 max-w-2xl text-sm text-slate-500 dark:text-slate-400">
              {description}
            </p>
          )}
        </div>
        {action}
      </div>
      <div className="mt-8">{children}</div>
    </section>
  );
}
