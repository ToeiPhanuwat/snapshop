import type { RuleGroupReview } from "@/types/api";
import { RuleGroupReviewCard } from "./RuleGroupReviewCard";

/**
 * Rule groups (กลุ่ม R) with items waiting for review, then those with nothing waiting whose name groups can still be
 * merged: their items are already on the site under their own name groups.
 */
export function RuleGroupSections({ groups }: { groups: RuleGroupReview[] }) {
  return (
    <>
      <Section
        title="รอ REVIEW"
        note="มีสินค้าในกลุ่ม E ที่มีรายการเดียว ยังไม่ขึ้นเว็บจนกว่าจะยืนยัน"
        empty="ไม่มีกลุ่ม R ที่รอตรวจ"
        groups={groups.filter((g) => g.pendingCount > 0)}
      />
      <Section
        title="ยุบรวมได้"
        note="สินค้าขึ้นเว็บแล้วในกลุ่ม E ของตัวเอง ยืนยันเพื่อรวมกลุ่ม E ที่ติ๊กเข้ากลุ่ม R"
        empty="ไม่มีกลุ่ม R ที่ยุบรวมได้"
        groups={groups.filter((g) => g.pendingCount === 0)}
      />
    </>
  );
}

function Section({
  title,
  note,
  empty,
  groups,
}: {
  title: string;
  note: string;
  empty: string;
  groups: RuleGroupReview[];
}) {
  return (
    <section aria-label={title} className="space-y-3">
      <div>
        <h2 className="text-base font-semibold">{title}</h2>
        <p className="text-xs text-zinc-500">{note}</p>
      </div>
      {groups.length === 0 ? (
        <p className="text-sm text-zinc-500">{empty}</p>
      ) : (
        groups.map((group) => <RuleGroupReviewCard key={group.code} group={group} />)
      )}
    </section>
  );
}
