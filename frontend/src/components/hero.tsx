import { ArrowRight, Sparkles } from "lucide-react";

export function Hero() {
  return (
    <section
      id="top"
      className="relative overflow-hidden bg-gradient-to-b from-emerald-50 via-white to-white dark:from-emerald-950/30 dark:via-slate-950 dark:to-slate-950"
    >
      <div
        aria-hidden
        className="pointer-events-none absolute inset-x-0 -top-40 flex justify-center blur-3xl"
      >
        <div className="h-72 w-[36rem] rounded-full bg-emerald-400/30 dark:bg-emerald-500/20" />
      </div>

      <div className="relative mx-auto max-w-7xl px-6 pt-20 pb-24 text-center lg:px-8">
        <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-600/20 bg-emerald-50 px-3 py-1 text-xs font-medium text-emerald-700 dark:border-emerald-400/20 dark:bg-emerald-500/10 dark:text-emerald-400">
          <Sparkles className="h-3.5 w-3.5" />
          Full lifecycle payment orchestration
        </span>

        <h1 className="mx-auto mt-6 max-w-3xl text-4xl font-bold tracking-tight text-slate-900 sm:text-5xl dark:text-white">
          Payment processing, built for{" "}
          <span className="bg-gradient-to-r from-emerald-600 to-teal-500 bg-clip-text text-transparent">
            reliability at scale
          </span>
        </h1>

        <p className="mx-auto mt-6 max-w-2xl text-base leading-relaxed text-slate-600 sm:text-lg dark:text-slate-300">
          RupeeX manages payments from creation through validation,
          processing, and completion — with a full audit trail of every
          status change along the way.
        </p>

        <div className="mt-10 flex flex-col items-center justify-center gap-3 sm:flex-row">
          <a
            href="#dashboard"
            className="inline-flex items-center justify-center gap-2 rounded-lg bg-emerald-600 px-5 py-2.5 text-sm font-semibold text-white shadow-sm shadow-emerald-600/30 transition hover:bg-emerald-700"
          >
            View dashboard
            <ArrowRight className="h-4 w-4" />
          </a>
          <a
            href="https://github.com"
            target="_blank"
            className="inline-flex items-center justify-center gap-2 rounded-lg border border-slate-200 bg-white px-5 py-2.5 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 dark:border-white/10 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800"
          >
            Read the docs
          </a>
        </div>
      </div>
    </section>
  );
}
