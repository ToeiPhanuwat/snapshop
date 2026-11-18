import Link from "next/link";
import { Suspense } from "react";
import { SearchForm } from "@/components/product/SearchForm";
import { Badge } from "@/components/ui/Badge";
import { Pagination } from "@/components/ui/Pagination";
import { getAdminTypes } from "@/lib/api/admin";

export default function AdminTypesPage({ searchParams }: PageProps<"/admin/types">) {
  return (
    <>
      <h1 className="text-lg font-semibold">กลุ่มสินค้าทั้งหมด</h1>
      <p className="text-sm text-zinc-500">
        กลุ่มที่มีรายการแสดงได้น้อยกว่า 2 รายการจะไม่แสดงฝั่ง user ย้ายรายการเข้ากลุ่มด้วยรหัสกลุ่มในหน้าสินค้า
      </p>
      <Suspense fallback={<p className="text-sm text-zinc-500">กำลังโหลด…</p>}>
        <Results searchParams={searchParams} />
      </Suspense>
    </>
  );
}

async function Results({ searchParams }: Pick<PageProps<"/admin/types">, "searchParams">) {
  const { q, page } = await searchParams;
  const text = typeof q === "string" && q ? q : undefined;
  const types = await getAdminTypes({ q: text, page: Math.max(0, Number(page ?? 0) || 0) });
  return (
    <>
      <SearchForm action="/admin/types" defaultValue={text} placeholder="ค้นหาชื่อกลุ่มหรือรหัส" />
      <p className="text-sm text-zinc-500">{types.total} กลุ่ม</p>
      <div className="overflow-x-auto rounded-xl bg-white ring-1 ring-zinc-200">
        <table className="w-full text-left text-sm">
          <thead className="text-xs text-zinc-500">
            <tr>
              <th className="p-3">รหัส</th>
              <th className="p-3">ชื่อกลุ่ม</th>
              <th className="p-3 text-right">รายการปัจจุบัน</th>
              <th className="p-3 text-right">แสดงได้</th>
              <th className="p-3">ฝั่ง user</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-100">
            {types.items.map((t) => (
              <tr key={t.code}>
                <td className="whitespace-nowrap p-3">
                  <Link className="text-orange-700 hover:underline" href={`/admin/products?typeCode=${t.code}`}>
                    {t.code}
                  </Link>
                </td>
                <td className="p-3">{t.name}</td>
                <td className="p-3 text-right">{t.activeCount}</td>
                <td className="p-3 text-right">{t.visibleCount}</td>
                <td className="p-3">{t.onUserSite ? <Badge tone="ok">แสดง</Badge> : <Badge>ไม่แสดง</Badge>}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Pagination path="/admin/types" params={{ q: text }} page={types.page} size={types.size} total={types.total} />
    </>
  );
}
