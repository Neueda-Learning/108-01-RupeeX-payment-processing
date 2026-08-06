import Link from "next/link";
import Image from "next/image";
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
    <footer className="border-t border-slate-200 bg-white/80 backdrop-blur-sm">
      <div className="mx-auto max-w-7xl px-6 py-10 lg:px-8">
        <div className="flex flex-col items-start justify-between gap-6 md:flex-row md:items-center">
          <div>
            <div className="flex items-center gap-3 font-semibold text-slate-900">
              <Image
                src="/rupeex-logo.svg"
                alt="RupeeX Logo"
                width={32}
                height={32}
                className="rounded-lg"
              />
              <div>
                <span className="block text-sm font-bold text-slate-900 leading-tight">RupeeX</span>
                <span className="block text-xs font-medium text-orange-600">Operations Console</span>
              </div>
            </div>
            <p className="mt-2 max-w-sm text-sm text-slate-600">
              Every role gets a focused view of payment traffic so operators can
              track, investigate, and act quickly.
            </p>
          </div>

          <div className="flex flex-wrap gap-2">
            {STACK.map((tech) => (
              <span
                key={tech}
                className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600"
              >
                {tech}
              </span>
            ))}
          </div>
        </div>

        <div className="mt-8 flex flex-col items-center justify-between gap-4 border-t border-slate-200 pt-6 text-sm text-slate-500 md:flex-row">
          <p>© {new Date().getFullYear()} RupeeX. All rights reserved.</p>
          <Link
            href="https://github.com"
            target="_blank"
            className="inline-flex items-center gap-1.5 hover:text-orange-600"
          >
            <ExternalLink className="h-4 w-4" />
            View source
          </Link>
        </div>
      </div>
    </footer>
  );
}
