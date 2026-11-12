"use client";

import { useActionState } from "react";
import { deactivateAction, moveAction, resolveAction } from "@/app/admin/actions";
import type { AdminProduct } from "@/types/api";
import { ActionMessage } from "./ActionMessage";

const smallButton = "rounded-md px-2 py-1 text-xs ring-1 ring-inset disabled:opacity-50";

/** Admin decisions for one item: resolve REVIEW, move to another group, mark discontinued. */
export function ProductActions({ product }: { product: AdminProduct }) {
  const [resolveState, resolve, resolving] = useActionState(resolveAction, null);
  const [moveState, move, moving] = useActionState(moveAction, null);
  const [deactivateState, deactivate, deactivating] = useActionState(deactivateAction, null);
  const itemId = <input type="hidden" name="itemId" value={product.itemId} />;

  return (
    <div className="space-y-2">
      <div className="flex flex-wrap items-center gap-2">
        {product.status === "REVIEW" ? (
          <form action={resolve}>
            {itemId}
            <button disabled={resolving} className={`${smallButton} ring-emerald-300 text-emerald-700`}>
              ตรวจแล้ว
            </button>
          </form>
        ) : null}
        <form action={move} className="flex items-center gap-1">
          {itemId}
          <input
            name="productTypeCode"
            placeholder="PT000001"
            aria-label="ย้ายไปกลุ่ม"
            className="w-24 rounded-md border border-zinc-300 px-2 py-1 text-xs"
          />
          <button disabled={moving} className={`${smallButton} ring-zinc-300`}>
            ย้ายกลุ่ม
          </button>
        </form>
        <form
          action={deactivate}
          onSubmit={(event) => {
            if (!confirm(`ยืนยันว่าสินค้า ${product.itemId} เลิกขายจริง? จะหายจากเว็บทั้งสองฝั่ง`)) {
              event.preventDefault();
            }
          }}
        >
          {itemId}
          <button disabled={deactivating} className={`${smallButton} ring-rose-300 text-rose-700`}>
            เลิกขาย
          </button>
        </form>
      </div>
      <ActionMessage state={resolveState} />
      <ActionMessage state={moveState} />
      <ActionMessage state={deactivateState} />
    </div>
  );
}
