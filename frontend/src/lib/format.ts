import type { ProductStatus, ShopType } from "@/types/api";

const baht = new Intl.NumberFormat("th-TH", { style: "currency", currency: "THB", maximumFractionDigits: 2 });
const dateTime = new Intl.DateTimeFormat("th-TH", { dateStyle: "medium", timeStyle: "short", timeZone: "Asia/Bangkok" });

export function formatPrice(price: number | null): string {
  return price === null ? "ไม่ทราบราคา" : baht.format(price);
}

export function formatPriceRange(min: number, max: number): string {
  return min === max ? formatPrice(min) : `${formatPrice(min)} – ${formatPrice(max)}`;
}

export function formatDateTime(iso: string): string {
  return dateTime.format(new Date(iso));
}

export const SHOP_TYPE_LABEL: Record<ShopType, string> = {
  MALL: "Mall",
  PREFERRED: "ร้านแนะนำ",
  REGULAR: "ทั่วไป",
};

export const STATUS_LABEL: Record<ProductStatus, string> = {
  OK: "ปกติ",
  NOT_IN_LATEST: "ไม่อยู่ในรอบล่าสุด",
  TEST: "สินค้าทดสอบ",
  OUT_OF_STOCK: "สินค้าหมด",
  REVIEW: "รอตรวจ",
};
