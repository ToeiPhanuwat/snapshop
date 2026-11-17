// @vitest-environment node
import { NextRequest } from "next/server";
import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { config, proxy } from "./proxy";

const adminLogin = "Basic " + Buffer.from("admin:s3cret").toString("base64");

function requestTo(path: string, authorization?: string): NextRequest {
  return new NextRequest(`http://localhost:3000${path}`, {
    headers: authorization ? { authorization } : {},
  });
}

beforeEach(() => {
  vi.stubEnv("ADMIN_USERNAME", "admin");
  vi.stubEnv("ADMIN_PASSWORD", "s3cret");
});

afterEach(() => {
  vi.unstubAllEnvs();
});

describe("proxy", () => {
  test("asks the browser for the admin login when there is none", async () => {
    const response = proxy(requestTo("/admin"));
    expect(response.status).toBe(401);
    expect(response.headers.get("WWW-Authenticate")).toBe('Basic realm="snapshop admin", charset="UTF-8"');
    expect(await response.text()).toBe("ต้องเข้าสู่ระบบผู้ดูแล");
  });

  test("refuses a wrong login", () => {
    const wrong = "Basic " + Buffer.from("admin:guess").toString("base64");
    expect(proxy(requestTo("/admin/products", wrong)).status).toBe(401);
  });

  test("lets the admin through", () => {
    const response = proxy(requestTo("/admin", adminLogin));
    expect(response.status).toBe(200);
    expect(response.headers.get("x-middleware-next")).toBe("1");
  });

  test("guards /admin and every page under it", () => {
    expect(config.matcher).toEqual(["/admin", "/admin/:path*"]);
  });
});
