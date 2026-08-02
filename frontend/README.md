# RupeeX Frontend

Next.js (App Router, TypeScript, Tailwind CSS v4) dashboard UI for the
RupeeX payment processing platform.

## Stack

- **Next.js 16** (App Router, Turbopack)
- **React 19**
- **TypeScript** (strict mode)
- **Tailwind CSS v4**
- **lucide-react** for icons

## Project structure

```
src/
  app/
    layout.tsx      # Root layout: fonts, Navbar, Footer
    page.tsx        # Dashboard home page (stats, payments, accounts, features)
    globals.css     # Tailwind entry + theme tokens
  components/       # Reusable UI components (Navbar, Hero, StatCard, ...)
  lib/
    types.ts        # Types mirroring backend entities (Payment, Account, ...)
    api.ts          # Fetch wrapper for the Spring Boot backend
    mock-data.ts     # Demo data used until backend endpoints exist
    format.ts        # Currency/date/status formatting helpers
```

## Getting started

```bash
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000).

## Connecting to the backend

Copy the env example and point it at your running Spring Boot API:

```bash
cp .env.local.example .env.local
```

```
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

`src/lib/api.ts` calls `GET /api/payments`, `GET /api/accounts`, and
`GET /api/dashboard/stats`. Until those controllers exist on the backend,
the UI automatically falls back to demo data from `src/lib/mock-data.ts`
so the dashboard always renders.

## Scripts

| Command         | Description                       |
| ---------------- | ---------------------------------- |
| `npm run dev`    | Start the dev server (Turbopack)   |
| `npm run build`  | Production build                   |
| `npm run start`  | Run the production build           |
| `npm run lint`   | Run ESLint                          |

## Conventions

- Components are named in `kebab-case.tsx` and export a single named component.
- Keep backend-mirrored types in `src/lib/types.ts` in sync with
  `com.rupeex.main.model.*`.
- Prefer server components; use `"use client"` only where interactivity
  (state, event handlers) is required.

