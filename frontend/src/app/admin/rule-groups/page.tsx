import { Suspense } from "react";
import { RuleGroupReviewCard } from "@/components/admin/RuleGroupReviewCard";
import { getRuleGroups } from "@/lib/api/admin";

export default function AdminRuleGroupsPage() {
  return (
    <>
      <h1 className="text-lg font-semibold">REVIEW กลุ่ม R</h1>
      <p className="text-sm text-zinc-500">
        สินค้าที่อยู่ในกลุ่มตามกฎคำระบุสูตร (กลุ่ม R) ยังไม่ขึ้นเว็บจนกว่าจะยืนยันที่นี่ กลุ่ม E ที่ติ๊กจะรวมเข้ากลุ่ม R
        ทั้งกลุ่มและแสดงบนเว็บด้วยชื่อกลุ่ม R ส่วนที่ไม่ติ๊กแสดงในกลุ่ม E เดิม
      </p>
      <Suspense fallback={<p className="text-sm text-zinc-500">กำลังโหลด…</p>}>
        <Groups />
      </Suspense>
    </>
  );
}

async function Groups() {
  const groups = await getRuleGroups();
  if (groups.length === 0) {
    return <p className="text-sm text-zinc-500">ไม่มีกลุ่ม R ที่รอตรวจ</p>;
  }
  return (
    <div className="space-y-4">
      {groups.map((group) => (
        <RuleGroupReviewCard key={group.code} group={group} />
      ))}
    </div>
  );
}
