import Link from "next/link";
import { Badge } from "@/components/ui/Badge";
import { ShopTypeBadge } from "@/components/product/ShopTypeBadge";
import { formatDateTime, formatPrice } from "@/lib/format";
import type { AdminProduct } from "@/types/api";
import { ProductActions } from "./ProductActions";
import { StatusBadge } from "./StatusBadge";

export function AdminProductTable({ products }: { products: AdminProduct[] }) {
  if (products.length === 0) {
    return <p className="text-sm text-zinc-500">ไม่มีรายการ</p>;
  }
  return (
    <ul className="divide-y divide-zinc-100 rounded-xl bg-white ring-1 ring-zinc-200">
      {products.map((p) => (
        <li key={p.productId} className="grid gap-3 p-4 lg:grid-cols-[1fr_auto]">
          <div className="min-w-0 space-y-1">
            <p className="text-sm">{p.displayName}</p>
            <div className="flex flex-wrap items-center gap-2 text-xs text-zinc-500">
              <StatusBadge status={p.status} />
              {p.onUserSite ? <Badge tone="ok">แสดงฝั่ง user</Badge> : <Badge>ไม่แสดงฝั่ง user</Badge>}
              <ShopTypeBadge shopType={p.shopType} />
              <span>{p.shopName}</span>
              <span>· {formatPrice(p.price)}</span>
              <span>· item {p.itemId}</span>
              <span>· เห็นล่าสุด {formatDateTime(p.lastSeenAt)}</span>
            </div>
            <p className="text-xs text-zinc-500">
              กลุ่ม{" "}
              <Link className="text-orange-700 hover:underline" href={`/admin/products?typeCode=${p.productTypeCode}`}>
                {p.productTypeCode}
              </Link>{" "}
              {p.typeName} ·{" "}
              <a className="hover:underline" href={p.link} target="_blank" rel="noopener noreferrer">
                เปิดหน้าสินค้า
              </a>
            </p>
          </div>
          <ProductActions product={p} />
        </li>
      ))}
    </ul>
  );
}
