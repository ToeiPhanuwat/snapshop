import { expect, test } from "@playwright/test";
import {
  adminLogin,
  AQUA_GEL,
  AQUA_GEL_BOX,
  AQUA_GEL_SMALL,
  COQ10,
  TONER,
  TONER_BIG,
  TONER_SMALL,
} from "./support";

test.use({ httpCredentials: adminLogin() });

test.describe.configure({ mode: "serial" });

test("rule items wait off the site only when their name group cannot show on its own", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("heading", { name: COQ10 })).toBeVisible();
  await expect(page.getByRole("heading", { name: TONER_SMALL })).toBeVisible();
  await expect(page.getByRole("heading", { name: TONER_BIG })).toBeVisible();
  await expect(page.getByRole("heading", { name: AQUA_GEL })).toHaveCount(0);
  await expect(page.getByRole("heading", { name: AQUA_GEL_SMALL })).toHaveCount(0);
});

test("the admin merges the name groups into the rule group, and shoppers compare them under its name", async ({
  page,
}) => {
  await page.goto("/admin/rule-groups");
  const waiting = page.getByRole("region", { name: "รอ REVIEW" });
  const card = waiting.locator("form", { has: page.getByRole("heading", { name: AQUA_GEL }) });
  await expect(card.getByRole("group", { name: AQUA_GEL_SMALL })).toContainText("(1ชิ้น) Dr.PONG E2E Aqua Gel 10 g");
  await expect(card.getByRole("group", { name: AQUA_GEL_BOX })).toContainText("อยู่ในกลุ่ม R");
  const ticks = card.getByRole("checkbox");
  await expect(ticks).toHaveCount(2);
  for (const tick of await ticks.all()) {
    await expect(tick).toBeChecked();
  }
  // A rule group in one name group is never listed.
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

  await expect(waiting.getByText("ไม่มีกลุ่ม R ที่รอตรวจ")).toBeVisible();

  await page.goto("/");
  await page.getByRole("link", { name: new RegExp(AQUA_GEL) }).click();
  await expect(page.getByRole("heading", { level: 1, name: AQUA_GEL })).toBeVisible();
  await expect(page.getByText("2 ร้าน · ฿250.00 – ฿1,200.00")).toBeVisible();
});

test("a rule group with nothing waiting can still be merged, and the old link leads to it", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("link", { name: new RegExp(TONER_SMALL) }).click();
  await expect(page.getByRole("heading", { level: 1, name: TONER_SMALL })).toBeVisible();
  const oldLink = page.url();

  await page.goto("/admin/rule-groups");
  const mergeable = page.getByRole("region", { name: "ยุบรวมได้" });
  const card = mergeable.locator("form", { has: page.getByRole("heading", { name: TONER }) });
  await expect(card).not.toContainText(/รอตรวจ \d+ รายการ/);
  await expect(card.getByRole("checkbox")).toHaveCount(2);
  await card.getByRole("button", { name: "ยืนยัน" }).click();
  await expect(mergeable.getByText("ไม่มีกลุ่ม R ที่ยุบรวมได้")).toBeVisible();

  await page.goto(oldLink);
  await expect(page.getByRole("heading", { level: 1, name: TONER })).toBeVisible();
  await expect(page).not.toHaveURL(oldLink);
  await expect(page.getByText("4 ร้าน · ฿190.00 – ฿350.00")).toBeVisible();
});

test("the group list shows the merge", async ({ page }) => {
  await page.goto(`/admin/types?q=${encodeURIComponent("Aqua Gel")}`);
  await expect(page.getByRole("row").filter({ hasText: AQUA_GEL_BOX })).toContainText("กลุ่ม E · รวมเข้า PT");
  await expect(page.getByRole("row").filter({ hasText: AQUA_GEL }).filter({ hasText: "กลุ่ม R" })).toHaveCount(1);
});
