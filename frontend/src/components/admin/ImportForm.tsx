"use client";

import { useActionState } from "react";
import { importAction } from "@/app/admin/actions";
import { ActionMessage } from "./ActionMessage";

export function ImportForm() {
  const [state, action, pending] = useActionState(importAction, null);
  return (
    <form action={action} className="space-y-3 rounded-xl bg-white p-4 ring-1 ring-zinc-200">
      <label className="block space-y-1 text-sm">
        <span className="font-medium">ไฟล์ CSV ที่ merge และจัดกลุ่มแล้ว</span>
        <input type="file" name="file" accept=".csv,text/csv" required className="block w-full text-sm" />
      </label>
      <p className="text-xs text-zinc-500">
        ต้องมีคอลัมน์ รหัสสินค้า, ชื่อสินค้า (ชื่อเต็ม), ราคา, …, รหัสกลุ่ม, ชื่อกลุ่ม, วิธีจัดกลุ่ม — ทั้งไฟล์นำเข้าในครั้งเดียว ถ้ามีแถวผิดจะไม่บันทึกอะไรเลย
      </p>
      <button
        type="submit"
        disabled={pending}
        className="rounded-lg bg-orange-600 px-4 py-2 text-sm font-medium text-white hover:bg-orange-700 disabled:opacity-50"
      >
        {pending ? "กำลังนำเข้า…" : "นำเข้า"}
      </button>
      <ActionMessage state={state} />
      {state?.result ? (
        <dl className="grid grid-cols-2 gap-x-4 gap-y-1 text-sm sm:grid-cols-3">
          <dt className="text-zinc-500">แถวในไฟล์</dt><dd>{state.result.rowCount}</dd>
          <dt className="text-zinc-500">รายการใหม่</dt><dd>{state.result.insertedCount}</dd>
          <dt className="text-zinc-500">ราคาเปลี่ยน</dt><dd>{state.result.priceChangedCount}</dd>
          <dt className="text-zinc-500">ราคาเท่าเดิม</dt><dd>{state.result.unchangedCount}</dd>
          <dt className="text-zinc-500">ไม่อยู่ในรอบนี้ (ซ่อน)</dt><dd>{state.result.notInLatestCount}</dd>
          <dt className="text-zinc-500">กลุ่มใหม่</dt><dd>{state.result.newTypeCount}</dd>
        </dl>
      ) : null}
    </form>
  );
}
