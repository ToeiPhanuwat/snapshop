import Link from "next/link";
import { Suspense } from "react";
import { AdminProductTable } from "@/components/admin/AdminProductTable";
import { FeedSyncPanel } from "@/components/admin/FeedSyncPanel";
import { ImportHistory } from "@/components/admin/ImportHistory";
import { getAdminProducts, getFeedSyncs, getInbox, getRecentImports } from "@/lib/api/admin";

const loading = <p className="text-sm text-zinc-500">กำลังโหลด…</p>;

export default function AdminHome() {
  return (
    <>
      <Suspense fallback={null}>
        <InboxCount />
      </Suspense>
      <section className="space-y-3">
        <h1 className="text-lg font-semibold">รอตรวจ (REVIEW)</h1>
        <p className="text-sm text-zinc-500">
          รายการใหม่ที่ไฟล์จัดไว้กับสมาชิกจากหลายกลุ่ม — ตรวจแล้วกดปิดได้เมื่อมีราคา
        </p>
        <Suspense fallback={loading}>
          <Products status="REVIEW" />
        </Suspense>
      </section>
      <section className="space-y-3">
        <h2 className="text-lg font-semibold">รอราคาจากฟีด</h2>
        <p className="text-sm text-zinc-500">
          ราคาในไฟล์เป็นตัวย่อ (เช่น &quot;1.0พัน&quot;) ระบบดึงราคาขายจากฟีดให้เองหลังนำเข้า ซ่อนจากเว็บ user จนกว่าจะได้ราคา
        </p>
        <Suspense fallback={loading}>
          <Products status="PRICE_PENDING" />
        </Suspense>
        <Suspense fallback={loading}>
          <FeedSyncs />
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

async function Products({ status }: { status: "REVIEW" | "NOT_IN_LATEST" | "PRICE_PENDING" }) {
  const page = await getAdminProducts({ status });
  return <AdminProductTable products={page.items} />;
}

async function InboxCount() {
  const count = (await getInbox()).length;
  if (count === 0) {
    return null;
  }
  return (
    <Link href="/admin/inbox" className="block rounded-xl bg-orange-50 p-4 text-sm text-orange-800 ring-1 ring-orange-200">
      มีไฟล์รอนำเข้า {count} ไฟล์ — ดูพรีวิวและนำเข้า →
    </Link>
  );
}

async function FeedSyncs() {
  return <FeedSyncPanel runs={await getFeedSyncs()} />;
}

async function RecentImports() {
  return <ImportHistory imports={(await getRecentImports()).slice(0, 5)} />;
}
