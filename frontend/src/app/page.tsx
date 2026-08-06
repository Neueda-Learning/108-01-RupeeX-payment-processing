import { MemberRedirect } from "@/components/member-redirect";
import { TeamHero } from "@/components/team-hero";

export default function Home() {
  return (
    <div className="mx-auto max-w-7xl px-6 py-16 lg:px-8">
      <MemberRedirect to="/accounts" />
      <TeamHero />
    </div>
  );
}
