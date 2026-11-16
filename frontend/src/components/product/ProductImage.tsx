"use client";

import { useState } from "react";

/**
 * Product picture in a square frame. Without a picture, or when the picture fails to load, a quiet placeholder
 * with the words "ยังไม่มีรูป" takes its place, so a card never shows a broken image.
 */
export function ProductImage({ src, alt }: { src: string | null; alt: string }) {
  const [failed, setFailed] = useState(false);
  if (!src || failed) {
    return (
      <div className="flex size-full flex-col items-center justify-center gap-1 bg-zinc-100 text-zinc-400">
        <svg viewBox="0 0 24 24" className="size-8" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
          <rect x="3" y="4" width="18" height="16" rx="2" />
          <circle cx="9" cy="10" r="1.5" />
          <path d="m21 16-5-5-9 9" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
        <span className="text-xs">ยังไม่มีรูป</span>
      </div>
    );
  }
  return (
    // eslint-disable-next-line @next/next/no-img-element -- shop images come from the feed's own host; no resizing proxy is wanted
    <img
      src={src}
      alt={alt}
      loading="lazy"
      referrerPolicy="no-referrer"
      onError={() => setFailed(true)}
      className="size-full object-cover"
    />
  );
}
