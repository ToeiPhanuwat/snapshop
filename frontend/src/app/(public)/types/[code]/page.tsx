import Link from "next/link";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import { PriceTable } from "@/components/product/PriceTable";
import { getProductType } from "@/lib/api/public";
import { formatPriceRange } from "@/lib/format";

export default function ProductTypePage({ params }: PageProps<"/types/[code]">) {
  return (
    <div className="space-y-4">
      <Link href="/" className="text-sm text-zinc-500 hover:text-zinc-900">
        ← สินค้าทั้งหมด
      </Link>
      <Suspense fallback={<p className="text-sm text-zinc-500">กำลังโหลด…</p>}>
        <TypeOffers params={params} />
      </Suspense>
    </div>
  );
}

async function TypeOffers({ params }: Pick<PageProps<"/types/[code]">, "params">) {
  const { code } = await params;
  const type = await getProductType(code);
  if (!type) {
    notFound();
  }
  return (
    <>
      <div className="space-y-1">
        <h1 className="text-xl font-semibold">{type.name}</h1>
        <p className="text-sm text-zinc-500">
          {type.offerCount} ร้าน · {formatPriceRange(type.minPrice, type.maxPrice)}
        </p>
      </div>
      <PriceTable offers={type.offers} />
    </>
  );
}
