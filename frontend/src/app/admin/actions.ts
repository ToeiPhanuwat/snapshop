"use server";

import { refresh } from "next/cache";
import { importFile, markDiscontinued, moveProduct, resolveReview } from "@/lib/api/admin";
import type { Result } from "@/lib/api/http";
import type { ImportResult } from "@/types/api";

/** What an admin form shows after its action ran. */
export type ActionState = {
  ok: boolean;
  message: string;
  errors?: string[];
  result?: ImportResult;
} | null;

function failure(result: Extract<Result<unknown>, { ok: false }>): ActionState {
  return { ok: false, message: result.problem.detail ?? `เกิดข้อผิดพลาด (${result.status})`, errors: result.problem.errors };
}

function itemIdOf(formData: FormData): number {
  const itemId = Number(formData.get("itemId"));
  if (!Number.isSafeInteger(itemId) || itemId <= 0) {
    throw new Error("itemId ไม่ถูกต้อง");
  }
  return itemId;
}

export async function importAction(_: ActionState, formData: FormData): Promise<ActionState> {
  const file = formData.get("file");
  if (!(file instanceof File) || file.size === 0) {
    return { ok: false, message: "กรุณาเลือกไฟล์ CSV" };
  }
  const result = await importFile(file);
  if (!result.ok) {
    return failure(result);
  }
  refresh();
  return { ok: true, message: "นำเข้าสำเร็จ", result: result.data };
}

export async function resolveAction(_: ActionState, formData: FormData): Promise<ActionState> {
  const result = await resolveReview(itemIdOf(formData));
  if (!result.ok) {
    return failure(result);
  }
  refresh();
  return { ok: true, message: "ตรวจแล้ว" };
}

export async function moveAction(_: ActionState, formData: FormData): Promise<ActionState> {
  const code = String(formData.get("productTypeCode") ?? "").trim().toUpperCase();
  if (!code) {
    return { ok: false, message: "กรุณาใส่รหัสกลุ่ม เช่น PT000001" };
  }
  const result = await moveProduct(itemIdOf(formData), code);
  if (!result.ok) {
    return failure(result);
  }
  refresh();
  return { ok: true, message: `ย้ายไป ${code} แล้ว` };
}

export async function deactivateAction(_: ActionState, formData: FormData): Promise<ActionState> {
  const result = await markDiscontinued(itemIdOf(formData));
  if (!result.ok) {
    return failure(result);
  }
  refresh();
  return { ok: true, message: "บันทึกว่าเลิกขายแล้ว" };
}
