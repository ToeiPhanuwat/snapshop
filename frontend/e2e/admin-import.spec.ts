import { expect, test } from "@playwright/test";
import { adminLogin, NIGHT_CREAM, sampleFile, SERUM } from "./support";

test.use({ httpCredentials: adminLogin() });

// Runs first (project "seed"): every other test reads the data this import writes.
test("the admin imports the sample file and sees what changed", async ({ page }) => {
  await page.goto("/admin/import");
  await page.getByLabel(/ไฟล์ CSV/).setInputFiles(sampleFile);

  // Hold the Server Action's answer for a moment, to see the button while the import runs.
  let release!: () => void;
  const held = new Promise<void>((resolve) => (release = resolve));
  await page.route("**/admin/import", async (route) => {
    if (route.request().method() === "POST") {
      await held;
    }
    await route.continue();
  });

  const button = page.getByRole("button", { name: /นำเข้า/ });
  await button.click();
  await expect(button).toBeDisabled();
  await expect(button).toHaveText("กำลังนำเข้า…");
  release();

  await expect(page.getByRole("status")).toHaveText("นำเข้าสำเร็จ");
  const counts = page.locator("form dl");
  await expect(counts.getByText("แถวในไฟล์").locator("+ dd")).toHaveText("11");
  await expect(counts.getByText("รายการใหม่").locator("+ dd")).toHaveText("11");
  await expect(counts.getByText("กลุ่มใหม่").locator("+ dd")).toHaveText("6");
  await expect(button).toBeEnabled();
  await expect(page.getByRole("cell", { name: "e2e-import.csv" })).toBeVisible();
});

test("the item whose price is abbreviated waits for the feed price", async ({ page }) => {
  await page.goto("/admin");
  const pending = page.locator("section", { has: page.getByRole("heading", { name: "รอราคาจากฟีด" }) });
  await expect(pending.getByText(`${SERUM} ขวดใหญ่`)).toBeVisible();
  await expect(pending.getByText("รอราคาจากฟีด", { exact: true }).first()).toBeVisible();
  await expect(pending.getByText(NIGHT_CREAM)).toHaveCount(0);
});
