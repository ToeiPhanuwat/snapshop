"use client";

import { useActionState } from "react";
import { discardInboxAction, importInboxAction } from "@/app/admin/actions";
import { ActionMessage } from "./ActionMessage";

/** The admin's decision on one inbox file: import it, or discard it. */
export function InboxDecision({ name, canImport }: { name: string; canImport: boolean }) {
  const [importState, importFile, importing] = useActionState(importInboxAction, null);
  const [discardState, discard, discarding] = useActionState(discardInboxAction, null);
  const fileName = <input type="hidden" name="name" value={name} />;

  return (
    <div className="space-y-2">
      <div className="flex flex-wrap gap-2">
        {canImport ? (
          <form action={importFile}>
            {fileName}
            <button
              disabled={importing}
              className="rounded-lg bg-orange-600 px-4 py-2 text-sm font-medium text-white hover:bg-orange-700 disabled:opacity-50"
            >
              {importing ? "กำลังนำเข้า…" : "นำเข้าไฟล์นี้"}
            </button>
          </form>
        ) : null}
        <form
          action={discard}
          onSubmit={(event) => {
            if (!confirm(`ทิ้งไฟล์ ${name}? ไฟล์จะย้ายไปโฟลเดอร์ rejected/ โดยไม่นำเข้า`)) {
              event.preventDefault();
            }
          }}
        >
          {fileName}
          <button
            disabled={discarding}
            className="rounded-lg px-4 py-2 text-sm text-rose-700 ring-1 ring-inset ring-rose-300 disabled:opacity-50"
          >
            ทิ้งไฟล์
          </button>
        </form>
      </div>
      <ActionMessage state={importState} />
      <ActionMessage state={discardState} />
    </div>
  );
}
