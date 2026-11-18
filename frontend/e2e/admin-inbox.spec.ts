import { expect, test } from "@playwright/test";
import { adminLogin, putInInbox } from "./support";

test.use({ httpCredentials: adminLogin() });

test.describe.configure({ mode: "serial" });

test.beforeAll(() => {
  // The same rows as the first import, so importing them again hides nothing the other tests look at.
  putInInbox("e2e-again.csv");
  putInInbox("e2e-discard.csv");
});

test("the overview tells the admin that files wait in the inbox", async ({ page }) => {
  await page.goto("/admin");
  await page.getByRole("link", { name: "มีไฟล์รอนำเข้า 2 ไฟล์ — ดูพรีวิวและนำเข้า →" }).click();
  await expect(page).toHaveURL(/\/admin\/inbox$/);
  await expect(page.getByText("e2e-again.csv")).toBeVisible();
  await expect(page.getByText("e2e-discard.csv")).toBeVisible();
});

test("the admin previews a file, imports it and comes back to the inbox", async ({ page }) => {
  await page.goto("/admin/inbox");
  await page.getByRole("listitem").filter({ hasText: "e2e-again.csv" }).getByRole("link", { name: "ดูพรีวิว" }).click();

  await expect(page.getByRole("heading", { name: "e2e-again.csv" })).toBeVisible();
  await expect(page.getByText("แถวในไฟล์").locator("+ dd")).toHaveText("11");
  await expect(page.getByText("รอ REVIEW กลุ่ม R").locator("+ dd")).toHaveText("0");
  await page.getByRole("button", { name: "นำเข้าไฟล์นี้" }).click();

  await expect(page).toHaveURL(/\/admin\/inbox\?done=/);
  await expect(page.getByRole("status")).toContainText("นำเข้า e2e-again.csv แล้ว");
  // By role: Next keeps the preview page it left in the DOM, hidden, and getByText would still count it.
  await expect(page.getByRole("listitem").filter({ hasText: "e2e-again.csv" })).toHaveCount(0);
  await expect(page.getByRole("listitem").filter({ hasText: "e2e-discard.csv" })).toHaveCount(1);
});

test("the admin discards a file after confirming", async ({ page }) => {
  await page.goto(`/admin/inbox/${encodeURIComponent("e2e-discard.csv")}`);
  page.once("dialog", (dialog) => dialog.accept());
  await page.getByRole("button", { name: "ทิ้งไฟล์" }).click();

  await expect(page).toHaveURL(/\/admin\/inbox\?done=/);
  await expect(page.getByRole("status")).toHaveText("ทิ้งไฟล์ e2e-discard.csv แล้ว (ย้ายไป rejected/)");
  await expect(page.getByText("ไม่มีไฟล์รอนำเข้า")).toBeVisible();
});
