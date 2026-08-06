import Link from "next/link";
import { ExternalLink } from "lucide-react";

const STACK = [
  "Admin View",
  "Source Account View",
  "Destination Account View",
  "Live Events",
  "Fraud Rules",
  "DLQ Recovery",
];

export function Footer() {
  return (
    <footer className="border-t border-slate-200 dark:border-slate-800 bg-white/80 dark:bg-slate-900/80 backdrop-blur-sm">
      <div className="mx-auto max-w-7xl px-6 py-10 lg:px-8">
        <div className="flex flex-col items-start justify-between gap-6 md:flex-row md:items-center">
          <div>
            <div className="flex items-center gap-2 font-semibold text-slate-900 dark:text-white">
              <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-orange-500 dark:bg-orange-600 text-sm text-white">
                ₹
              </span>
              RupeeX Operations Console
            </div>
            <p className="mt-2 max-w-sm text-sm text-slate-600 dark:text-slate-400">
              Every role gets a focused view of payment traffic so operators can
              track, investigate, and act quickly.
            </p>
          </div>

          <div className="flex flex-wrap gap-2">
            {STACK.map((tech) => (
              <span
                key={tech}
                className="rounded-full bg-slate-100 dark:bg-slate-800 px-3 py-1 text-xs font-medium text-slate-600 dark:text-slate-400"
              >
                {tech}
              </span>
            ))}
          </div>
        </div>

        <div className="mt-8 flex flex-col items-center justify-between gap-4 border-t border-slate-200 dark:border-slate-800 pt-6 text-sm text-slate-500 dark:text-slate-400 md:flex-row">
          <p>© {new Date().getFullYear()} RupeeX. All rights reserved.</p>
          <Link
            href="https://github.com"
            target="_blank"
            className="inline-flex items-center gap-1.5 hover:text-orange-600 dark:hover:text-orange-400"
          >
            <ExternalLink className="h-4 w-4" />
            View source
          </Link>
        </div>
      </div>
    </footer>
  );
}
