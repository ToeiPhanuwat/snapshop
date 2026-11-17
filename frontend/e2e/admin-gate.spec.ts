import { expect, test } from "@playwright/test";

test.describe("the admin pages need the admin login", () => {
  for (const path of ["/admin", "/admin/inbox", "/admin/import"]) {
    test(`${path} without a login asks for one`, async ({ request }) => {
      const response = await request.get(path);
      expect(response.status()).toBe(401);
      expect(response.headers()["www-authenticate"]).toContain('Basic realm="snapshop admin"');
    });
  }

  test("a wrong password is refused", async ({ request }) => {
    const wrong = "Basic " + Buffer.from(`${process.env.ADMIN_USERNAME}:not-the-password`).toString("base64");
    const response = await request.get("/admin", { headers: { Authorization: wrong } });
    expect(response.status()).toBe(401);
  });

  test("the shop pages are open to everyone", async ({ request }) => {
    expect((await request.get("/")).status()).toBe(200);
  });
});
