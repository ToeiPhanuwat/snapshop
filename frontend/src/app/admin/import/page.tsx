import { Suspense } from "react";
import { ImportForm } from "@/components/admin/ImportForm";
import { ImportHistory } from "@/components/admin/ImportHistory";
import { getRecentImports } from "@/lib/api/admin";

export default function ImportPage() {
  return (
    <>
      <h1 className="text-lg font-semibold">นำเข้าไฟล์</h1>
      <ImportForm />
      <h2 className="text-base font-semibold">ประวัติการนำเข้า</h2>
      <Suspense fallback={<p className="text-sm text-zinc-500">กำลังโหลด…</p>}>
        <History />
      </Suspense>
    </>
  );
}

async function History() {
  return <ImportHistory imports={await getRecentImports()} />;
}
