// @vitest-environment node
import { headers } from "next/headers";
import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import {
  confirmRuleGroup,
  discardInbox,
  getAdminProducts,
  getAdminTypes,
  getFeedSyncs,
  getInbox,
  getRecentImports,
  getRuleGroups,
  importFile,
  importInbox,
  markDiscontinued,
  moveProduct,
  previewInbox,
  resolveReview,
  retryFeedSync,
} from "./admin";

vi.mock("next/headers", () => ({ headers: vi.fn() }));

const adminLogin = "Basic " + Buffer.from("admin:s3cret").toString("base64");
const fetch = vi.fn<typeof globalThis.fetch>();

/** The browser's request to the Next.js server carries this Authorization header. */
function callerSends(authorization: string | null) {
  vi.mocked(headers).mockResolvedValue(new Headers(authorization ? { authorization } : {}) as never);
}

const json = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });

/** The URL, method and headers of the n-th backend call. */
function backendCall(n = 0) {
  const [url, init] = fetch.mock.calls[n];
  return { url, method: init?.method ?? "GET", headers: new Headers(init?.headers), body: init?.body };
}

beforeEach(() => {
  vi.stubEnv("ADMIN_USERNAME", "admin");
  vi.stubEnv("ADMIN_PASSWORD", "s3cret");
  vi.stubEnv("BACKEND_URL", "http://backend:8080");
  vi.stubGlobal("fetch", fetch);
  fetch.mockReset();
  callerSends(adminLogin);
});

afterEach(() => {
  vi.unstubAllEnvs();
  vi.unstubAllGlobals();
});

describe("admin gate", () => {
  test.each([
    ["no login", null],
    ["a wrong login", "Basic " + Buffer.from("admin:guess").toString("base64")],
  ])("refuses a caller with %s before asking the backend", async (_, authorization) => {
    callerSends(authorization);
    await expect(getInbox()).rejects.toThrow("Unauthorized");
    await expect(retryFeedSync()).rejects.toThrow("Unauthorized");
    expect(fetch).not.toHaveBeenCalled();
  });

  test("sends the admin's own login to the backend", async () => {
    fetch.mockResolvedValue(json(200, []));
    await getInbox();
    expect(backendCall().headers.get("Authorization")).toBe(adminLogin);
  });
});

describe("reads", () => {
  test("getAdminProducts filters on the backend, 100 at a time", async () => {
    fetch.mockResolvedValue(json(200, { items: [], page: 0, size: 100, total: 0 }));
    await getAdminProducts({ status: "REVIEW", q: "sun", page: 2 });
    expect(backendCall().url).toBe("http://backend:8080/api/admin/products?status=REVIEW&q=sun&page=2&size=100");
  });

  test("getAdminTypes searches the groups, 100 at a time", async () => {
    fetch.mockResolvedValue(json(200, { items: [], page: 0, size: 100, total: 0 }));
    await getAdminTypes({ q: "toner" });
    expect(backendCall().url).toBe("http://backend:8080/api/admin/product-types?q=toner&size=100");
  });

  test.each([
    ["getRecentImports", getRecentImports, "/api/admin/imports"],
    ["getInbox", getInbox, "/api/admin/inbox"],
    ["getFeedSyncs", getFeedSyncs, "/api/admin/feed-syncs"],
    ["getRuleGroups", getRuleGroups, "/api/admin/rule-groups"],
  ] as const)("%s returns what the backend lists", async (_, read, path) => {
    fetch.mockResolvedValue(json(200, [{ id: 1 }]));
    expect(await read()).toEqual([{ id: 1 }]);
    expect(backendCall()).toMatchObject({ url: `http://backend:8080${path}`, method: "GET" });
  });

  test("a failed read throws to the error page", async () => {
    fetch.mockResolvedValue(json(500, { detail: "DB ล่ม" }));
    await expect(getInbox()).rejects.toThrow("Backend 500 for /api/admin/inbox: DB ล่ม");
  });

  test("a failed read without a reason still names the status", async () => {
    fetch.mockResolvedValue(json(502, {}));
    await expect(getFeedSyncs()).rejects.toThrow(/^Backend 502 for \/api\/admin\/feed-syncs: $/);
  });
});

describe("writes", () => {
  test("importFile uploads the file as form data", async () => {
    fetch.mockResolvedValue(json(200, { batchId: 1 }));
    const csv = new File(["รหัสสินค้า\n1"], "drpong.csv", { type: "text/csv" });

    expect(await importFile(csv)).toEqual({ ok: true, data: { batchId: 1 } });
    const call = backendCall();
    expect(call).toMatchObject({ url: "http://backend:8080/api/admin/imports", method: "POST" });
    expect(((call.body as FormData).get("file") as File).name).toBe("drpong.csv");
    expect(call.headers.get("Authorization")).toBe(adminLogin);
  });

  test("confirmRuleGroup sends the ticked name groups as JSON", async () => {
    fetch.mockResolvedValue(new Response(null, { status: 204 }));
    expect(await confirmRuleGroup("PT 3", ["PT000011"])).toEqual({ ok: true, data: undefined });
    const call = backendCall();
    expect(call).toMatchObject({ url: "http://backend:8080/api/admin/rule-groups/PT%203/confirm", method: "POST" });
    expect(call.headers.get("Content-Type")).toBe("application/json");
    expect(call.headers.get("Authorization")).toBe(adminLogin);
    expect(JSON.parse(call.body as string)).toEqual({ mergeTypeCodes: ["PT000011"] });
  });

  test("moveProduct sends the new group as JSON", async () => {
    fetch.mockResolvedValue(json(200, {}));
    await moveProduct(1001, "PT000002");
    const call = backendCall();
    expect(call).toMatchObject({ url: "http://backend:8080/api/admin/products/1001/move", method: "POST" });
    expect(call.headers.get("Content-Type")).toBe("application/json");
    expect(call.headers.get("Authorization")).toBe(adminLogin);
    expect(JSON.parse(call.body as string)).toEqual({ productTypeCode: "PT000002" });
  });

  test.each([
    ["resolveReview", () => resolveReview(1001), "/api/admin/products/1001/resolve"],
    ["markDiscontinued", () => markDiscontinued(1001), "/api/admin/products/1001/deactivate"],
    ["importInbox", () => importInbox("a b/c.csv"), "/api/admin/inbox/a%20b%2Fc.csv/import"],
    ["discardInbox", () => discardInbox("a b/c.csv"), "/api/admin/inbox/a%20b%2Fc.csv/discard"],
    ["retryFeedSync", () => retryFeedSync(), "/api/admin/feed-syncs"],
  ] as const)("%s posts to the backend", async (_, write, path) => {
    fetch.mockResolvedValue(new Response(null, { status: 204 }));
    expect(await write()).toEqual({ ok: true, data: undefined });
    expect(backendCall()).toMatchObject({ url: `http://backend:8080${path}`, method: "POST" });
  });

  test("previewInbox reads the preview of the encoded file name", async () => {
    fetch.mockResolvedValue(json(200, { fileName: "a b.csv" }));
    expect(await previewInbox("a b.csv")).toEqual({ ok: true, data: { fileName: "a b.csv" } });
    expect(backendCall()).toMatchObject({ url: "http://backend:8080/api/admin/inbox/a%20b.csv/preview", method: "GET" });
  });

  test("a refused write is returned to the caller, not thrown", async () => {
    fetch.mockResolvedValue(json(409, { detail: "มีการดึงอยู่แล้ว" }));
    expect(await retryFeedSync()).toEqual({ ok: false, status: 409, problem: { detail: "มีการดึงอยู่แล้ว" } });
  });
});
