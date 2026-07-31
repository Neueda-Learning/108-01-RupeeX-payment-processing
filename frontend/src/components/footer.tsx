import Link from "next/link";
import { ExternalLink } from "lucide-react";

const STACK = ["Next.js", "React", "Tailwind CSS", "Spring Boot", "MySQL", "Docker"];

export function Footer() {
  return (
    <footer className="border-t border-black/5 bg-white dark:border-white/10 dark:bg-slate-950">
      <div className="mx-auto max-w-7xl px-6 py-10 lg:px-8">
        <div className="flex flex-col items-start justify-between gap-6 md:flex-row md:items-center">
          <div>
            <div className="flex items-center gap-2 font-semibold text-slate-900 dark:text-white">
              <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-emerald-600 text-sm text-white">
                ₹
              </span>
              RupeeX
            </div>
            <p className="mt-2 max-w-sm text-sm text-slate-500 dark:text-slate-400">
              A complete payment processing system handling the full
              lifecycle of financial payments — creation, validation,
              processing, and audit.
            </p>
          </div>

          <div className="flex flex-wrap gap-2">
            {STACK.map((tech) => (
              <span
                key={tech}
                className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600 dark:bg-slate-800 dark:text-slate-300"
              >
                {tech}
              </span>
            ))}
          </div>
        </div>

        <div className="mt-8 flex flex-col items-center justify-between gap-4 border-t border-black/5 pt-6 text-sm text-slate-500 md:flex-row dark:border-white/10 dark:text-slate-400">
          <p>© {new Date().getFullYear()} RupeeX. All rights reserved.</p>
          <Link
            href="https://github.com"
            target="_blank"
            className="inline-flex items-center gap-1.5 hover:text-emerald-600 dark:hover:text-emerald-400"
          >
            <ExternalLink className="h-4 w-4" />
            View source
          </Link>
        </div>
      </div>
    </footer>
  );
}
