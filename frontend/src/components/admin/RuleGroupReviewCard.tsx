"use client";

import { useActionState } from "react";
import { confirmRuleGroupAction } from "@/app/admin/actions";
import { Badge } from "@/components/ui/Badge";
import { formatPrice } from "@/lib/format";
import type { RuleGroupReview, RuleNameGroup } from "@/types/api";
import { ActionMessage } from "./ActionMessage";
import { StatusBadge } from "./StatusBadge";

/**
 * One rule group (กลุ่ม R) next to the name groups (กลุ่ม E) its items sit in. Ticked name groups merge into it whole;
 * confirming passes every item of the rule group, ticked or not.
 */
export function RuleGroupReviewCard({ group }: { group: RuleGroupReview }) {
  const [state, confirm, confirming] = useActionState(confirmRuleGroupAction, null);
  return (
    <form action={confirm} className="space-y-3 rounded-xl bg-white p-4 ring-1 ring-zinc-200">
      <input type="hidden" name="code" value={group.code} />
      <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1">
        <h2 className="font-semibold">{group.name}</h2>
        <span className="text-xs text-zinc-500">{group.code}</span>
        <Badge tone="warn">รอตรวจ {group.pendingCount} รายการ</Badge>
      </div>
      <p className="text-xs text-zinc-500">ติ๊กกลุ่ม E ที่จะรวมเข้ากลุ่มนี้ (รวมทั้งกลุ่ม) แล้วกดยืนยัน</p>
      <div className="space-y-3">
        {group.nameGroups.map((nameGroup) => (
          <NameGroupBlock key={nameGroup.code} ruleCode={group.code} nameGroup={nameGroup} />
        ))}
      </div>
      <div className="flex items-center gap-3">
        <button
          disabled={confirming}
          className="rounded-md bg-zinc-900 px-3 py-1.5 text-sm text-white disabled:opacity-50"
        >
          {confirming ? "กำลังยืนยัน…" : "ยืนยัน"}
        </button>
        <ActionMessage state={state} />
      </div>
    </form>
  );
}

function NameGroupBlock({ ruleCode, nameGroup }: { ruleCode: string; nameGroup: RuleNameGroup }) {
  const mergedHere = nameGroup.mergedIntoCode === ruleCode;
  const mergedElsewhere = nameGroup.mergedIntoCode !== null && !mergedHere;
  return (
    <fieldset className="rounded-lg ring-1 ring-zinc-200">
      <legend className="sr-only">{nameGroup.name}</legend>
      <label className="flex flex-wrap items-center gap-2 border-b border-zinc-100 p-2 text-sm">
        <input
          type="checkbox"
          name="merge"
          value={nameGroup.code}
          defaultChecked={!mergedElsewhere}
          disabled={nameGroup.mergedIntoCode !== null}
          aria-label={`รวม ${nameGroup.code}`}
        />
        <span className="font-medium">{nameGroup.name}</span>
        <span className="text-xs text-zinc-500">{nameGroup.code}</span>
        {mergedHere ? <Badge tone="ok">รวมเข้ากลุ่มนี้แล้ว</Badge> : null}
        {mergedElsewhere ? <Badge>รวมเข้า {nameGroup.mergedIntoCode} แล้ว</Badge> : null}
      </label>
      <ul className="divide-y divide-zinc-100 text-sm">
        {nameGroup.items.map((item) => (
          <li key={item.itemId} className="flex flex-wrap items-center gap-x-3 gap-y-1 p-2">
            <span className="text-xs text-zinc-500">{item.itemId}</span>
            <span className={item.inRule ? "" : "text-zinc-500"}>{item.displayName}</span>
            <span className="text-xs text-zinc-500">{item.shopName}</span>
            <span>{formatPrice(item.price)}</span>
            <StatusBadge status={item.status} />
            {item.inRule ? <Badge tone="info">อยู่ในกลุ่ม R</Badge> : null}
          </li>
        ))}
      </ul>
    </fieldset>
  );
}
