// @vitest-environment node
import { afterEach, describe, expect, test, vi } from "vitest";
import { backendUrl, query, request, requestOrNull } from "./http";

afterEach(() => {
  vi.unstubAllEnvs();
  vi.unstubAllGlobals();
});

/** Replaces fetch with one that answers `response` and records what was asked. */
function backendAnswers(response: Response) {
  const fetch = vi.fn<typeof globalThis.fetch>().mockResolvedValue(response);
  vi.stubGlobal("fetch", fetch);
  return fetch;
}

const json = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });

describe("request", () => {
  test("asks the backend without caching, keeping the caller's options", async () => {
    vi.stubEnv("BACKEND_URL", "http://backend:8080");
    const fetch = backendAnswers(json(200, { items: [] }));

    expect(await request("/api/product-types", { method: "POST" })).toEqual({ ok: true, data: { items: [] } });
    expect(fetch).toHaveBeenCalledWith("http://backend:8080/api/product-types", { method: "POST", cache: "no-store" });
  });

  test("an empty 204 answer is a success", async () => {
    backendAnswers(new Response(null, { status: 204 }));
    expect(await request("/api/x")).toEqual({ ok: true, data: undefined });
  });

  test("a failure carries its status and problem", async () => {
    backendAnswers(json(400, { detail: "ไฟล์มีแถวผิด", errors: ["แถว 2"] }));
    expect(await request("/api/x")).toEqual({
      ok: false,
      status: 400,
      problem: { detail: "ไฟล์มีแถวผิด", errors: ["แถว 2"] },
    });
  });

  test("a failure whose body is not JSON still reports its status", async () => {
    backendAnswers(new Response("<html>Bad Gateway</html>", { status: 502 }));
    expect(await request("/api/x")).toEqual({ ok: false, status: 502, problem: {} });
  });
});

describe("requestOrNull", () => {
  test("returns the data", async () => {
    backendAnswers(json(200, { code: "PT000001" }));
    expect(await requestOrNull("/api/product-types/PT000001")).toEqual({ code: "PT000001" });
  });

  test("returns null for something that does not exist", async () => {
    backendAnswers(json(404, { detail: "ไม่พบ" }));
    expect(await requestOrNull("/api/product-types/PT999999")).toBeNull();
  });

  test("throws any other failure to the error page", async () => {
    backendAnswers(json(500, { detail: "DB ล่ม" }));
    await expect(requestOrNull("/api/product-types")).rejects.toThrow("Backend 500 for /api/product-types: DB ล่ม");
  });

  test("throws without a reason when the backend gives none", async () => {
    backendAnswers(json(503, {}));
    await expect(requestOrNull("/api/x")).rejects.toThrow(/^Backend 503 for \/api\/x: $/);
  });
});

describe("query", () => {
  test("leaves out empty, null and undefined values", () => {
    expect(query({ q: "dr", page: 2, empty: "", none: null, missing: undefined })).toBe("?q=dr&page=2");
  });

  test("keeps zero, which is a real page number", () => {
    expect(query({ page: 0 })).toBe("?page=0");
  });

  test("is empty when no value is left", () => {
    expect(query({ q: "", page: undefined })).toBe("");
  });

  test("encodes the values so they read back the same", () => {
    const text = query({ q: "ครีม & กันแดด" });
    expect(text).not.toContain(" ");
    expect(new URLSearchParams(text).get("q")).toBe("ครีม & กันแดด");
  });
});

describe("backendUrl", () => {
  test("joins BACKEND_URL and the path without a double slash", () => {
    vi.stubEnv("BACKEND_URL", "http://backend:8080/");
    expect(backendUrl("/api/product-types")).toBe("http://backend:8080/api/product-types");
  });

  test("uses the local backend when BACKEND_URL is not set", () => {
    vi.stubEnv("BACKEND_URL", undefined);
    expect(backendUrl("/api/x")).toBe("http://localhost:8080/api/x");
  });
});
