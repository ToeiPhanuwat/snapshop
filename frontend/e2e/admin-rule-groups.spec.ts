import { expect, test } from "@playwright/test";
import { adminLogin, AQUA_GEL, AQUA_GEL_BOX, AQUA_GEL_SMALL, COQ10 } from "./support";

test.use({ httpCredentials: adminLogin() });

test.describe.configure({ mode: "serial" });

test("items of a rule group wait off the site, unless their rule and name groups agree", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("heading", { name: COQ10 })).toBeVisible();
  await expect(page.getByRole("heading", { name: AQUA_GEL })).toHaveCount(0);
  await expect(page.getByRole("heading", { name: AQUA_GEL_SMALL })).toHaveCount(0);
});

test("the admin merges the name groups into the rule group, and shoppers compare them under its name", async ({
  page,
}) => {
  await page.goto("/admin/rule-groups");
  const card = page.locator("form", { has: page.getByRole("heading", { name: AQUA_GEL }) });
  await expect(card.getByRole("group", { name: AQUA_GEL_SMALL })).toContainText("(1ชิ้น) Dr.PONG E2E Aqua Gel 10 g");
  await expect(card.getByRole("group", { name: AQUA_GEL_BOX })).toContainText("อยู่ในกลุ่ม R");
  const ticks = card.getByRole("checkbox");
  await expect(ticks).toHaveCount(2);
  for (const tick of await ticks.all()) {
    await expect(tick).toBeChecked();
  }
  // The rule group whose name group agrees passed on import, so it is not here.
  await expect(page.getByRole("heading", { name: "E2E CoQ10" })).toHaveCount(0);

  // Hold the Server Action's answer for a moment, to see the button while it runs.
  let release!: () => void;
  const held = new Promise<void>((resolve) => (release = resolve));
  await page.route("**/admin/rule-groups", async (route) => {
    if (route.request().method() === "POST") {
      await held;
    }
    await route.continue();
  });
  await card.getByRole("button", { name: "ยืนยัน" }).click();
  await expect(card.getByRole("button", { name: "กำลังยืนยัน…" })).toBeDisabled();
  release();

  await expect(page.getByText("ไม่มีกลุ่ม R ที่รอตรวจ")).toBeVisible();

  await page.goto("/");
  await page.getByRole("link", { name: new RegExp(AQUA_GEL) }).click();
  await expect(page.getByRole("heading", { level: 1, name: AQUA_GEL })).toBeVisible();
  await expect(page.getByText("2 ร้าน · ฿250.00 – ฿1,200.00")).toBeVisible();
});

test("the group list shows the merge", async ({ page }) => {
  await page.goto(`/admin/types?q=${encodeURIComponent("Aqua Gel")}`);
  await expect(page.getByRole("row").filter({ hasText: AQUA_GEL_BOX })).toContainText("กลุ่ม E · รวมเข้า PT");
  await expect(page.getByRole("row").filter({ hasText: AQUA_GEL }).filter({ hasText: "กลุ่ม R" })).toHaveCount(1);
});
