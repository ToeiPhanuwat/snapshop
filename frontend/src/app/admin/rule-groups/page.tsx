import { Suspense } from "react";
import { RuleGroupSections } from "@/components/admin/RuleGroupSections";
import { getRuleGroups } from "@/lib/api/admin";

export default function AdminRuleGroupsPage() {
  return (
    <>
      <h1 className="text-lg font-semibold">REVIEW กลุ่ม R</h1>
      <p className="text-sm text-zinc-500">
        สินค้าขึ้นเว็บตามกลุ่ม E ที่มีตั้งแต่ 2 รายการ กลุ่ม E ที่ติ๊กจะรวมเข้ากลุ่ม R ทั้งกลุ่มและแสดงบนเว็บด้วยชื่อกลุ่ม R
        (ลิงก์กลุ่ม E เดิมพาไปหน้ากลุ่ม R) ส่วนที่ไม่ติ๊กแสดงในกลุ่ม E เดิม
      </p>
      <Suspense fallback={<p className="text-sm text-zinc-500">กำลังโหลด…</p>}>
        <Groups />
      </Suspense>
    </>
  );
}

async function Groups() {
  return <RuleGroupSections groups={await getRuleGroups()} />;
}
