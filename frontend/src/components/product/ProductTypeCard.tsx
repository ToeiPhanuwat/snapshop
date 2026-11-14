import Link from "next/link";
import { formatPriceRange } from "@/lib/format";
import type { ProductTypeSummary } from "@/types/api";

export function ProductTypeCard({ type }: { type: ProductTypeSummary }) {
  return (
    <Link
      href={`/types/${type.code}`}
      className="flex h-full flex-col justify-between gap-3 rounded-xl bg-white p-4 ring-1 ring-zinc-200 transition hover:ring-orange-300"
    >
      <h2 className="line-clamp-3 text-sm font-medium text-zinc-900">{type.name}</h2>
      <div className="flex items-end justify-between gap-2">
        <span className="text-base font-semibold text-orange-600">{formatPriceRange(type.minPrice, type.maxPrice)}</span>
        <span className="shrink-0 text-xs text-zinc-500">{type.offerCount} ร้าน</span>
      </div>
    </Link>
  );
}
