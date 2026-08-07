import Link from "next/link";
import { Code2 } from "lucide-react";

export function Footer() {
  const sourceUrl = process.env.NEXT_PUBLIC_SOURCE_URL || "https://github.com";

  return (
    <footer className="border-t border-slate-200/80 bg-white/60 backdrop-blur-md">
      <div className="mx-auto flex max-w-7xl items-center justify-between px-6 py-3.5 text-xs font-medium text-slate-500">
        <p>© 2026 RupeeX. All rights reserved.</p>
        <Link
          href={sourceUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="inline-flex items-center gap-1.5 transition-colors hover:text-slate-900"
        >
          <Code2 className="h-3.5 w-3.5 text-slate-400 group-hover:text-slate-600" />
          <span>Source</span>
        </Link>
      </div>
    </footer>
  );
}
