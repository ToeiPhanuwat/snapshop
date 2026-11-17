import { expect, type Page, test } from "@playwright/test";
import { adminLogin, NIGHT_CREAM, SUNSCREEN } from "./support";

test.use({ httpCredentials: adminLogin() });

/** The group code (PT000000) the shop page gives a product type. */
async function groupCodeOf(page: Page, typeName: string): Promise<string> {
  await page.goto("/");
  const href = await page.getByRole("link", { name: new RegExp(typeName) }).getAttribute("href");
  const code = href?.match(/PT\d{6}/)?.[0];
  if (!code) {
    throw new Error(`No group code in the link of ${typeName}: ${href}`);
  }
  return code;
}

/** The admin's row of one item: search the products page (by name, shop or group), then pick the item id. */
async function itemRow(page: Page, search: string, itemId: string) {
  await page.goto(`/admin/products?q=${encodeURIComponent(search)}`);
  const row = page.getByRole("listitem").filter({ hasText: `item ${itemId}` });
  await expect(row).toHaveCount(1);
  return row;
}

test.describe.configure({ mode: "serial" });

test("the admin moves an item into another group, and shoppers see it there", async ({ page }) => {
  const sunscreen = await groupCodeOf(page, SUNSCREEN);
  const row = await itemRow(page, NIGHT_CREAM, "900000000007");

  await row.getByRole("textbox", { name: "ย้ายไปกลุ่ม" }).fill(sunscreen.toLowerCase());
  await row.getByRole("button", { name: "ย้ายกลุ่ม" }).click();
  await expect(row.getByRole("status")).toHaveText(`ย้ายไป ${sunscreen} แล้ว`);

  await page.goto(`/types/${sunscreen}`);
  await expect(page.getByRole("listitem").filter({ hasText: NIGHT_CREAM })).toHaveCount(1);
});

test("the admin marks an item discontinued after confirming, and shoppers no longer see it", async ({ page }) => {
  const sunscreen = await groupCodeOf(page, SUNSCREEN);
  const row = await itemRow(page, "E2E Shop Gamma", "900000000003");

  // First the admin changes their mind: nothing happens.
  page.once("dialog", (dialog) => dialog.dismiss());
  await row.getByRole("button", { name: "เลิกขาย" }).click();
  await expect(row.getByRole("status")).toHaveCount(0);

  page.once("dialog", (dialog) => {
    expect(dialog.message()).toContain("900000000003");
    return dialog.accept();
  });
  await row.getByRole("button", { name: "เลิกขาย" }).click();
  // The item is no longer current, so it leaves the list (with its message) once the page data reloads.
  await expect(row).toHaveCount(0);

  await page.goto(`/types/${sunscreen}`);
  await expect(page.getByRole("heading", { level: 1, name: SUNSCREEN })).toBeVisible();
  await expect(page.getByText("E2E Shop Gamma")).toHaveCount(0);
});

test("the feed button says why it cannot look up when no feed link is set", async ({ page }) => {
  await page.goto("/admin");
  const button = page.getByRole("button", { name: "ดึงรายการที่ค้างจากฟีด" });
  await expect(button).toBeEnabled();
  await button.click();

  const answer = page.locator("form", { has: button }).getByRole("status");
  await expect(answer).not.toBeEmpty();
  await expect(answer).not.toContainText("เริ่มดึงจากฟีดแล้ว");
  await expect(button).toBeEnabled();
});
