import { MemberRedirect } from "@/components/member-redirect";

export default async function Home() {
  return (
    <div className="mx-auto max-w-7xl px-6 py-16 lg:px-8">
      <MemberRedirect to="/accounts" />

      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="text-center">
          <h1 className="text-5xl md:text-6xl font-bold tracking-tight text-slate-900">
            Group-01
          </h1>
          <p className="mt-6 text-2xl md:text-3xl text-slate-600 font-semibold">
            Team Name: <span className="text-orange-600">Skill Issue</span>
          </p>
        </div>
      </div>
    </div>
  );
}
