const TONES = {
  neutral: "bg-zinc-100 text-zinc-700 ring-zinc-200",
  brand: "bg-orange-50 text-orange-700 ring-orange-200",
  info: "bg-sky-50 text-sky-700 ring-sky-200",
  warn: "bg-amber-50 text-amber-800 ring-amber-200",
  danger: "bg-rose-50 text-rose-700 ring-rose-200",
  ok: "bg-emerald-50 text-emerald-700 ring-emerald-200",
} as const;

export type BadgeTone = keyof typeof TONES;

export function Badge({ tone = "neutral", children }: { tone?: BadgeTone; children: React.ReactNode }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ring-1 ring-inset ${TONES[tone]}`}>
      {children}
    </span>
  );
}
