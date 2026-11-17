import { expect, test } from "@playwright/test";
import { NIGHT_CREAM, SERUM, SUNSCREEN } from "./support";

test("the home page lists only the products that at least two shops sell", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("heading", { name: SUNSCREEN })).toBeVisible();
  await expect(page.getByRole("heading", { name: SERUM })).toBeVisible();
  // One shop only, so it is not a price comparison.
  await expect(page.getByRole("heading", { name: NIGHT_CREAM })).toHaveCount(0);
});

test("the search narrows the list and says when nothing matches", async ({ page }) => {
  await page.goto("/");
  const search = page.getByRole("search");
  await search.getByRole("searchbox").fill("Vitamin C");
  await search.getByRole("button", { name: "ค้นหา" }).click();

  await expect(page).toHaveURL(/\?q=Vitamin\+C/);
  await expect(page.getByRole("heading", { name: SERUM })).toBeVisible();
  await expect(page.getByRole("heading", { name: SUNSCREEN })).toHaveCount(0);

  await page.goto("/?q=ไม่มีสินค้านี้แน่นอน");
  await expect(page.getByText("ไม่พบสินค้าที่ตรงกับ “ไม่มีสินค้านี้แน่นอน”")).toBeVisible();
});

test("a product page compares the shops' prices, cheapest first", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("link", { name: new RegExp(SERUM) }).click();

  await expect(page).toHaveURL(/\/types\/PT\d{6}$/);
  await expect(page.getByRole("heading", { level: 1, name: SERUM })).toBeVisible();
  // The abbreviated price ("1.0พัน") is hidden until the feed gives the real one.
  await expect(page.getByText("2 ร้าน · ฿450.00 – ฿490.00")).toBeVisible();
  const offers = page.getByRole("listitem");
  await expect(offers).toHaveCount(2);
  await expect(offers.nth(0)).toContainText("E2E Shop Alpha");
  await expect(offers.nth(0)).toContainText("฿450.00");
  await expect(offers.nth(1)).toContainText("E2E Shop Delta");
  await expect(offers.nth(0).getByRole("link", { name: "ไปที่ร้าน" })).toHaveAttribute(
    "href",
    /e2e4|900000000004/,
  );
});

test("an unknown product code shows the not-found page", async ({ page }) => {
  await page.goto("/types/PT999999");
  await expect(page.getByRole("heading", { name: "ไม่พบหน้านี้" })).toBeVisible();
});
