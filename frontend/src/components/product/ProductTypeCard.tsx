import Link from "next/link";
import { formatPriceRange } from "@/lib/format";
import type { ProductTypeSummary } from "@/types/api";
import { ProductImage } from "./ProductImage";

export function ProductTypeCard({ type }: { type: ProductTypeSummary }) {
  return (
    <Link
      href={`/types/${type.code}`}
      className="flex h-full flex-col overflow-hidden rounded-xl bg-white ring-1 ring-zinc-200 transition hover:ring-orange-300"
    >
      <div className="relative aspect-square w-full overflow-hidden bg-zinc-100">
        <ProductImage src={type.imageUrl} alt={type.name} />
        <span className="absolute left-2 top-2 rounded-full bg-white/90 px-2 py-0.5 text-xs text-zinc-700 ring-1 ring-zinc-200">
          {type.offerCount} ร้าน
        </span>
      </div>
      <div className="flex flex-1 flex-col justify-between gap-2 p-3">
        <h2 className="line-clamp-2 min-h-10 text-sm font-medium text-zinc-900">{type.name}</h2>
        <span className="text-base font-semibold text-orange-600">{formatPriceRange(type.minPrice, type.maxPrice)}</span>
      </div>
    </Link>
  );
}
