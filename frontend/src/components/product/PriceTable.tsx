import { formatPrice } from "@/lib/format";
import type { Product } from "@/types/api";
import { ShopTypeBadge } from "./ShopTypeBadge";

/** Every shop's offer for one product type, in the order the API sends (cheapest first). */
export function PriceTable({ offers }: { offers: Product[] }) {
  const cheapest = Math.min(...offers.map((o) => o.price));
  return (
    <ul className="divide-y divide-zinc-100 overflow-hidden rounded-xl bg-white ring-1 ring-zinc-200">
      {offers.map((offer) => (
        <li key={offer.itemId} className="flex flex-col gap-3 p-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="min-w-0 space-y-1">
            <p className="text-sm text-zinc-900">{offer.displayName}</p>
            <div className="flex flex-wrap items-center gap-2 text-xs text-zinc-500">
              <ShopTypeBadge shopType={offer.shopType} />
              <span>{offer.shopName}</span>
              {offer.soldText ? <span>· ขายแล้ว {offer.soldText}</span> : null}
            </div>
          </div>
          <div className="flex shrink-0 items-center justify-between gap-4 sm:justify-end">
            <span className={`text-lg font-semibold ${offer.price === cheapest ? "text-orange-600" : "text-zinc-900"}`}>
              {formatPrice(offer.price)}
            </span>
            <a
              href={offer.link}
              target="_blank"
              rel="noopener noreferrer sponsored"
              className="rounded-lg bg-orange-600 px-4 py-2 text-sm font-medium text-white hover:bg-orange-700"
            >
              ไปที่ร้าน
            </a>
          </div>
        </li>
      ))}
    </ul>
  );
}
