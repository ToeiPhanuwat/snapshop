import { Suspense } from "react";
import { AdminProductTable } from "@/components/admin/AdminProductTable";
import { Pagination } from "@/components/ui/Pagination";
import { getAdminProducts } from "@/lib/api/admin";
import { STATUS_LABEL } from "@/lib/format";
import type { ProductStatus } from "@/types/api";

const STATUSES = Object.keys(STATUS_LABEL) as ProductStatus[];

type Filters = { status?: ProductStatus; typeCode?: string; q?: string; page: number };

function filtersFrom(params: Record<string, string | string[] | undefined>): Filters {
  const text = (key: string) => (typeof params[key] === "string" && params[key] ? (params[key] as string) : undefined);
  const status = text("status");
  return {
    status: STATUSES.includes(status as ProductStatus) ? (status as ProductStatus) : undefined,
    typeCode: text("typeCode"),
    q: text("q"),
    page: Math.max(0, Number(text("page") ?? 0) || 0),
  };
}

export default function AdminProductsPage({ searchParams }: PageProps<"/admin/products">) {
  return (
    <>
      <h1 className="text-lg font-semibold">สินค้าปัจจุบันทั้งหมด</h1>
      <Suspense fallback={<p className="text-sm text-zinc-500">กำลังโหลด…</p>}>
        <Results searchParams={searchParams} />
      </Suspense>
    </>
  );
}

async function Results({ searchParams }: Pick<PageProps<"/admin/products">, "searchParams">) {
  const filters = filtersFrom(await searchParams);
  const products = await getAdminProducts(filters);
  return (
    <>
      <form action="/admin/products" className="flex flex-wrap items-end gap-2 text-sm">
        <label className="space-y-1">
          <span className="block text-xs text-zinc-500">สถานะ</span>
          <select name="status" defaultValue={filters.status ?? ""} className="rounded-lg border border-zinc-300 bg-white px-2 py-2">
            <option value="">ทั้งหมด</option>
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                {STATUS_LABEL[s]}
              </option>
            ))}
          </select>
        </label>
        <label className="space-y-1">
          <span className="block text-xs text-zinc-500">รหัสกลุ่ม</span>
          <input name="typeCode" defaultValue={filters.typeCode} placeholder="PT000001" className="w-28 rounded-lg border border-zinc-300 px-2 py-2" />
        </label>
        <label className="min-w-48 flex-1 space-y-1">
          <span className="block text-xs text-zinc-500">ค้นหา (ชื่อสินค้า / ร้าน / กลุ่ม)</span>
          <input name="q" type="search" defaultValue={filters.q} className="w-full rounded-lg border border-zinc-300 px-2 py-2" />
        </label>
        <button className="rounded-lg bg-zinc-900 px-4 py-2 text-white">กรอง</button>
      </form>
      <p className="text-sm text-zinc-500">{products.total} รายการ</p>
      <AdminProductTable products={products.items} />
      <Pagination
        path="/admin/products"
        params={{ status: filters.status, typeCode: filters.typeCode, q: filters.q }}
        page={products.page}
        size={products.size}
        total={products.total}
      />
    </>
  );
}
