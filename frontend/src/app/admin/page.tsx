import Link from "next/link";
import { Suspense } from "react";
import { AdminProductTable } from "@/components/admin/AdminProductTable";
import { ImportHistory } from "@/components/admin/ImportHistory";
import { getAdminProducts, getRecentImports } from "@/lib/api/admin";

const loading = <p className="text-sm text-zinc-500">กำลังโหลด…</p>;

export default function AdminHome() {
  return (
    <>
      <section className="space-y-3">
        <h1 className="text-lg font-semibold">รอตรวจ (REVIEW)</h1>
        <p className="text-sm text-zinc-500">
          ราคาย่อจากไฟล์ (เช่น &quot;1.0พัน&quot;) หรือรายการใหม่ที่ไฟล์จัดไว้กับสมาชิกจากหลายกลุ่ม — ปิดได้เมื่อมีราคาแล้ว
        </p>
        <Suspense fallback={loading}>
          <Products status="REVIEW" />
        </Suspense>
      </section>
      <section className="space-y-3">
        <h2 className="text-lg font-semibold">ไม่อยู่ในรอบนำเข้าล่าสุด</h2>
        <p className="text-sm text-zinc-500">
          ซ่อนจากเว็บ user แล้ว แต่ยังเป็นแถวปัจจุบัน เปิดหน้าสินค้าเพื่อตรวจว่าเลิกขายจริงหรือไม่ ถ้าเลิกขายให้กด &quot;เลิกขาย&quot;
        </p>
        <Suspense fallback={loading}>
          <Products status="NOT_IN_LATEST" />
        </Suspense>
      </section>
      <section className="space-y-3">
        <h2 className="text-lg font-semibold">
          นำเข้าล่าสุด <Link href="/admin/import" className="text-sm font-normal text-orange-700 hover:underline">นำเข้าไฟล์ใหม่</Link>
        </h2>
        <Suspense fallback={loading}>
          <RecentImports />
        </Suspense>
      </section>
    </>
  );
}

async function Products({ status }: { status: "REVIEW" | "NOT_IN_LATEST" }) {
  const page = await getAdminProducts({ status });
  return <AdminProductTable products={page.items} />;
}

async function RecentImports() {
  return <ImportHistory imports={(await getRecentImports()).slice(0, 5)} />;
}
