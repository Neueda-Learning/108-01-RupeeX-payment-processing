"use client";

import { motion } from "framer-motion";

const container = {
  hidden: {},
  show: {
    transition: { staggerChildren: 0.12, delayChildren: 0.1 },
  },
};

const item = {
  hidden: { opacity: 0, y: 24 },
  show: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.6, ease: [0.16, 1, 0.3, 1] as const },
  },
};

/**
 * Big, creative team-branding hero for the home page: a huge translucent
 * "01" watermark behind a gradient "Skill Issue" headline, with a small
 * "Team 01" tag and a playful dev-humor subtitle.
 */
export function TeamHero() {
  return (
    <div className="relative flex min-h-[70vh] items-center justify-center overflow-hidden">
      {/* Giant watermark numeral */}
      <motion.span
        aria-hidden
        initial={{ opacity: 0, scale: 0.92 }}
        animate={{ opacity: [0.05, 0.1, 0.05], scale: 1 }}
        transition={{
          opacity: { duration: 6, repeat: Infinity, ease: "easeInOut" },
          scale: { duration: 0.8, ease: [0.16, 1, 0.3, 1] },
        }}
        className="pointer-events-none absolute -top-6 left-1/2 -translate-x-1/2 select-none text-[16rem] font-black leading-none tracking-tighter text-orange-500 sm:text-[22rem] md:text-[28rem]"
      >
        01
      </motion.span>

      <motion.div
        variants={container}
        initial="hidden"
        animate="show"
        className="relative z-10 flex flex-col items-center px-4 text-center"
      >
        <motion.span
          variants={item}
          className="inline-flex items-center gap-2 rounded-full border border-orange-200 bg-orange-50 px-4 py-1.5 font-mono text-xs font-semibold uppercase tracking-[0.3em] text-orange-700"
        >
          Team 01
        </motion.span>

        <motion.h1
          variants={item}
          className="mt-6 bg-gradient-to-br from-slate-900 via-orange-600 to-orange-400 bg-clip-text text-6xl font-black uppercase leading-[0.95] tracking-tight text-transparent sm:text-7xl md:text-8xl"
        >
          Skill Issue
        </motion.h1>

        <motion.p
          variants={item}
          className="mt-6 max-w-xl font-mono text-sm text-slate-500 sm:text-base"
        >
          <span className="text-slate-400">$</span> git commit -m{" "}
          <span className="text-orange-600">
            &quot;fix: it was never a bug, just a skill issue&quot;
          </span>
        </motion.p>

        <motion.div
          variants={item}
          className="mt-8 flex flex-wrap items-center justify-center gap-2 font-mono text-xs text-slate-400"
        >
          <span className="rounded-md bg-slate-900/5 px-2.5 py-1">
            RupeeX Payment Processing
          </span>
          <span className="rounded-md bg-slate-900/5 px-2.5 py-1">
            Built by Team 01
          </span>
        </motion.div>
      </motion.div>
    </div>
  );
}
