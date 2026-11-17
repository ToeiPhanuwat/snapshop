"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";

/** While `active`, reloads the page data every minute; it stops when inactive or when the page is left. */
export function AutoRefresh({ active, everyMs = 60_000 }: { active: boolean; everyMs?: number }) {
  const router = useRouter();
  useEffect(() => {
    if (!active) {
      return;
    }
    const timer = setInterval(() => router.refresh(), everyMs);
    return () => clearInterval(timer);
  }, [active, everyMs, router]);
  return null;
}
