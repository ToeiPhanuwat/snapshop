// @vitest-environment node
import { refresh } from "next/cache";
import { redirect } from "next/navigation";
import { beforeEach, describe, expect, test, vi } from "vitest";
import {
  confirmRuleGroup,
  discardInbox,
  importFile,
  importInbox,
  markDiscontinued,
  moveProduct,
  resolveReview,
  retryFeedSync,
} from "@/lib/api/admin";
import type { Result } from "@/lib/api/http";
import { importResult } from "@/test/fixtures";
import {
  confirmRuleGroupAction,
  deactivateAction,
  discardInboxAction,
  importAction,
  importInboxAction,
  moveAction,
  resolveAction,
  retryFeedAction,
} from "./actions";

vi.mock("@/lib/api/admin", () => ({
  confirmRuleGroup: vi.fn(),
  discardInbox: vi.fn(),
  importFile: vi.fn(),
  importInbox: vi.fn(),
  markDiscontinued: vi.fn(),
  moveProduct: vi.fn(),
  resolveReview: vi.fn(),
  retryFeedSync: vi.fn(),
}));
vi.mock("next/cache", () => ({ refresh: vi.fn() }));
// Like the real one, redirect ends the action by throwing.
vi.mock("next/navigation", () => ({
  redirect: vi.fn((url: string) => {
    throw new Error(`NEXT_REDIRECT ${url}`);
  }),
}));

const okResult = <T>(data: T): Result<T> => ({ ok: true, data });
const failed = (status: number, problem: { detail?: string; errors?: string[] } = {}): Result<never> => ({
  ok: false,
  status,
  problem,
});

function form(fields: Record<string, string | File>): FormData {
  const data = new FormData();
  for (const [key, value] of Object.entries(fields)) {
    data.set(key, value);
  }
  return data;
}

const csv = new File(["รหัสสินค้า\n1"], "drpong.csv", { type: "text/csv" });

/** Where an action that ends in redirect sent the browser, decoded. */
async function redirectedTo(run: Promise<unknown>): Promise<string> {
  await expect(run).rejects.toThrow("NEXT_REDIRECT");
  return decodeURIComponent(vi.mocked(redirect).mock.calls[0][0]);
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe("importAction", () => {
  test("asks for a file and calls nothing when none was chosen", async () => {
    expect(await importAction(null, form({}))).toEqual({ ok: false, message: "กรุณาเลือกไฟล์ CSV" });
    expect(await importAction(null, form({ file: new File([], "empty.csv") }))).toEqual({
      ok: false,
      message: "กรุณาเลือกไฟล์ CSV",
    });
    expect(await importAction(null, form({ file: "not a file" }))).toEqual({ ok: false, message: "กรุณาเลือกไฟล์ CSV" });
    expect(importFile).not.toHaveBeenCalled();
  });

  test("imports the file, reloads the page data and returns the counts", async () => {
    const result = importResult();
    vi.mocked(importFile).mockResolvedValue(okResult(result));

    expect(await importAction(null, form({ file: csv }))).toEqual({ ok: true, message: "นำเข้าสำเร็จ", result });
    expect(vi.mocked(importFile).mock.calls[0][0].name).toBe("drpong.csv");
    expect(refresh).toHaveBeenCalledTimes(1);
  });

  test("passes on why the backend rejected the file, without reloading", async () => {
    vi.mocked(importFile).mockResolvedValue(failed(400, { detail: "ไฟล์มีแถวผิด", errors: ["แถว 3: ไม่มีราคา"] }));

    expect(await importAction(null, form({ file: csv }))).toEqual({
      ok: false,
      message: "ไฟล์มีแถวผิด",
      errors: ["แถว 3: ไม่มีราคา"],
    });
    expect(refresh).not.toHaveBeenCalled();
  });

  test("names the status when the backend gives no reason", async () => {
    vi.mocked(importFile).mockResolvedValue(failed(500));
    expect(await importAction(null, form({ file: csv }))).toEqual({
      ok: false,
      message: "เกิดข้อผิดพลาด (500)",
      errors: undefined,
    });
  });
});

describe("item actions", () => {
  test("resolveAction marks the item reviewed", async () => {
    vi.mocked(resolveReview).mockResolvedValue(okResult(undefined as never));
    expect(await resolveAction(null, form({ itemId: "1001" }))).toEqual({ ok: true, message: "ตรวจแล้ว" });
    expect(resolveReview).toHaveBeenCalledWith(1001);
    expect(refresh).toHaveBeenCalledTimes(1);
  });

  test("deactivateAction marks the item discontinued", async () => {
    vi.mocked(markDiscontinued).mockResolvedValue(okResult(undefined));
    expect(await deactivateAction(null, form({ itemId: "1001" }))).toEqual({ ok: true, message: "บันทึกว่าเลิกขายแล้ว" });
    expect(markDiscontinued).toHaveBeenCalledWith(1001);
    expect(refresh).toHaveBeenCalledTimes(1);
  });

  test.each([
    ["resolveAction", resolveAction, resolveReview],
    ["deactivateAction", deactivateAction, markDiscontinued],
  ] as const)("%s passes on the backend's reason and does not reload", async (_, action, api) => {
    vi.mocked(api).mockResolvedValue(failed(409, { detail: "สถานะไม่ใช่ REVIEW" }));
    expect(await action(null, form({ itemId: "1001" }))).toEqual({
      ok: false,
      message: "สถานะไม่ใช่ REVIEW",
      errors: undefined,
    });
    expect(refresh).not.toHaveBeenCalled();
  });

  test.each(["0", "-1", "abc", "1.5", "", "9007199254740993"])("refuses the item id %j", async (itemId) => {
    await expect(resolveAction(null, form({ itemId }))).rejects.toThrow("itemId ไม่ถูกต้อง");
    await expect(deactivateAction(null, form({ itemId }))).rejects.toThrow("itemId ไม่ถูกต้อง");
    await expect(moveAction(null, form({ itemId, productTypeCode: "PT000001" }))).rejects.toThrow("itemId ไม่ถูกต้อง");
    expect(resolveReview).not.toHaveBeenCalled();
    expect(markDiscontinued).not.toHaveBeenCalled();
    expect(moveProduct).not.toHaveBeenCalled();
  });

  test("refuses a form without an item id", async () => {
    await expect(resolveAction(null, form({}))).rejects.toThrow("itemId ไม่ถูกต้อง");
  });
});

describe("moveAction", () => {
  test("moves the item to the group code, trimmed and upper-cased", async () => {
    vi.mocked(moveProduct).mockResolvedValue(okResult(undefined as never));
    expect(await moveAction(null, form({ itemId: "1001", productTypeCode: "  pt000002 " }))).toEqual({
      ok: true,
      message: "ย้ายไป PT000002 แล้ว",
    });
    expect(moveProduct).toHaveBeenCalledWith(1001, "PT000002");
    expect(refresh).toHaveBeenCalledTimes(1);
  });

  test.each<Record<string, string>>([{ productTypeCode: "   " }, {}])("asks for a group code when none was typed (%j)", async (fields) => {
    expect(await moveAction(null, form({ itemId: "1001", ...fields }))).toEqual({
      ok: false,
      message: "กรุณาใส่รหัสกลุ่ม เช่น PT000001",
    });
    expect(moveProduct).not.toHaveBeenCalled();
  });

  test("passes on why the move was refused", async () => {
    vi.mocked(moveProduct).mockResolvedValue(failed(404, { detail: "ไม่พบกลุ่ม PT999999" }));
    expect(await moveAction(null, form({ itemId: "1001", productTypeCode: "PT999999" }))).toMatchObject({
      ok: false,
      message: "ไม่พบกลุ่ม PT999999",
    });
    expect(refresh).not.toHaveBeenCalled();
  });
});

describe("inbox actions", () => {
  test("importInboxAction imports the file and goes back to the inbox with the counts", async () => {
    vi.mocked(importInbox).mockResolvedValue(
      okResult(importResult({ insertedCount: 12, priceChangedCount: 3, notInLatestCount: 7, newTypeCount: 2 })),
    );
    const url = await redirectedTo(importInboxAction(null, form({ name: "drpong 17/11.csv" })));
    expect(importInbox).toHaveBeenCalledWith("drpong 17/11.csv");
    expect(url).toBe("/admin/inbox?done=นำเข้า drpong 17/11.csv แล้ว: ใหม่ 12, ราคาเปลี่ยน 3, ซ่อน 7, กลุ่มใหม่ 2");
    expect(vi.mocked(redirect).mock.calls[0][0]).not.toContain(" ");
  });

  test("discardInboxAction discards the file and goes back to the inbox", async () => {
    vi.mocked(discardInbox).mockResolvedValue(okResult(undefined));
    const url = await redirectedTo(discardInboxAction(null, form({ name: "old.csv" })));
    expect(discardInbox).toHaveBeenCalledWith("old.csv");
    expect(url).toBe("/admin/inbox?done=ทิ้งไฟล์ old.csv แล้ว (ย้ายไป rejected/)");
  });

  test.each([
    ["importInboxAction", importInboxAction, importInbox],
    ["discardInboxAction", discardInboxAction, discardInbox],
  ] as const)("%s stays on the page and says why when the backend refuses", async (_, action, api) => {
    vi.mocked(api).mockResolvedValue(failed(404, { detail: "ไม่พบไฟล์" }));
    expect(await action(null, form({ name: "gone.csv" }))).toEqual({ ok: false, message: "ไม่พบไฟล์", errors: undefined });
    expect(redirect).not.toHaveBeenCalled();
  });

  test.each([
    ["importInboxAction", importInboxAction, importInbox],
    ["discardInboxAction", discardInboxAction, discardInbox],
  ] as const)("%s refuses a form without a file name", async (_, action, api) => {
    await expect(action(null, form({}))).rejects.toThrow("ไม่มีชื่อไฟล์");
    expect(api).not.toHaveBeenCalled();
  });
});

describe("retryFeedAction", () => {
  test("starts the look-up and reloads the page data", async () => {
    vi.mocked(retryFeedSync).mockResolvedValue(okResult(undefined));
    expect(await retryFeedAction()).toEqual({
      ok: true,
      message: "เริ่มดึงจากฟีดแล้ว ผลจะขึ้นในรายการด้านล่างเมื่อเสร็จ",
    });
    expect(refresh).toHaveBeenCalledTimes(1);
  });

  test("passes on why the look-up cannot start", async () => {
    vi.mocked(retryFeedSync).mockResolvedValue(failed(409, { detail: "ยังไม่ได้ตั้งลิงก์ feed" }));
    expect(await retryFeedAction()).toEqual({ ok: false, message: "ยังไม่ได้ตั้งลิงก์ feed", errors: undefined });
    expect(refresh).not.toHaveBeenCalled();
  });
});

describe("confirmRuleGroupAction", () => {
  test("sends the rule group and every ticked name group", async () => {
    vi.mocked(confirmRuleGroup).mockResolvedValue(okResult(undefined));
    const data = form({ code: "PT000003" });
    data.append("merge", "PT000011");
    data.append("merge", "PT000012");

    expect(await confirmRuleGroupAction(null, data)).toEqual({ ok: true, message: "ยืนยัน PT000003 แล้ว: รวม 2 กลุ่ม" });
    expect(confirmRuleGroup).toHaveBeenCalledWith("PT000003", ["PT000011", "PT000012"]);
    expect(refresh).toHaveBeenCalledTimes(1);
  });

  test("nothing ticked still confirms the rule group", async () => {
    vi.mocked(confirmRuleGroup).mockResolvedValue(okResult(undefined));
    await confirmRuleGroupAction(null, form({ code: "PT000003" }));
    expect(confirmRuleGroup).toHaveBeenCalledWith("PT000003", []);
  });

  test("a refusal is shown, not thrown", async () => {
    vi.mocked(confirmRuleGroup).mockResolvedValue(failed(409, { detail: "กลุ่ม PT000011 รวมเข้ากลุ่ม R อื่นแล้ว" }));
    expect(await confirmRuleGroupAction(null, form({ code: "PT000003" }))).toEqual({
      ok: false,
      message: "กลุ่ม PT000011 รวมเข้ากลุ่ม R อื่นแล้ว",
      errors: undefined,
    });
    expect(refresh).not.toHaveBeenCalled();
  });

  test("a form without the rule group code is a bug", async () => {
    await expect(confirmRuleGroupAction(null, form({}))).rejects.toThrow("ไม่มีรหัสกลุ่ม R");
    expect(confirmRuleGroup).not.toHaveBeenCalled();
  });
});
