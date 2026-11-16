"use client";

import { useActionState } from "react";
import { retryFeedAction } from "@/app/admin/actions";
import { Badge, type BadgeTone } from "@/components/ui/Badge";
import { formatDateTime } from "@/lib/format";
import type { FeedSyncOutcome, FeedSyncRun } from "@/types/api";
import { ActionMessage } from "./ActionMessage";

const OUTCOME: Record<FeedSyncOutcome, { label: string; tone: BadgeTone }> = {
  RUNNING: { label: "กำลังดึง", tone: "info" },
  DONE: { label: "เสร็จ", tone: "ok" },
  INCOMPLETE: { label: "ไม่ครบ", tone: "warn" },
  RATE_LIMITED: { label: "ถูกจำกัด (429)", tone: "danger" },
  FAILED: { label: "ล้มเหลว", tone: "danger" },
};

/** Results of the automatic look-ups in the feed, and a button to look up again what is still missing. */
export function FeedSyncPanel({ runs }: { runs: FeedSyncRun[] }) {
  const [state, action, pending] = useActionState(retryFeedAction, null);
  return (
    <div className="space-y-3">
      <form action={action} className="flex flex-wrap items-center gap-3">
        <button
          type="submit"
          disabled={pending}
          className="rounded-lg bg-orange-600 px-4 py-2 text-sm font-medium text-white hover:bg-orange-700 disabled:opacity-50"
        >
          {pending ? "กำลังส่งคำขอ…" : "ดึงรายการที่ค้างจากฟีด"}
        </button>
        <ActionMessage state={state} />
      </form>
      {runs.length === 0 ? (
        <p className="text-sm text-zinc-500">ยังไม่เคยดึงจากฟีด</p>
      ) : (
        <div className="overflow-x-auto rounded-xl bg-white ring-1 ring-zinc-200">
          <table className="w-full text-left text-sm">
            <thead className="text-xs text-zinc-500">
              <tr>
                <th className="p-3">เริ่ม</th>
                <th className="p-3">สถานะ</th>
                <th className="p-3 text-right">ขอ</th>
                <th className="p-3 text-right">เจอ</th>
                <th className="p-3">หมายเหตุ</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-100">
              {runs.slice(0, 5).map((run) => (
                <tr key={run.runId}>
                  <td className="whitespace-nowrap p-3">{formatDateTime(run.startedAt)}</td>
                  <td className="p-3">
                    <Badge tone={OUTCOME[run.outcome].tone}>{OUTCOME[run.outcome].label}</Badge>
                  </td>
                  <td className="p-3 text-right">{run.requestedCount}</td>
                  <td className="p-3 text-right">{run.foundCount}</td>
                  <td className="p-3 text-zinc-600">{run.message}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
