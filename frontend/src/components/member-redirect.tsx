"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useUserStore } from "@/lib/user-store";

/**
 * Invisible client component that redirects members to a given path.
 * Drop this into any server-rendered page that should be admin-only.
 */
export function MemberRedirect({ to }: { to: string }) {
  const { currentUser } = useUserStore();
  const router = useRouter();

  useEffect(() => {
    if (currentUser?.role === "member") {
      router.replace(to);
    }
  }, [currentUser, router, to]);

  return null;
}

