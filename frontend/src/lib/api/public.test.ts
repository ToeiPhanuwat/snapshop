// @vitest-environment node
import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { getProductType, getProductTypes } from "./public";

const fetch = vi.fn<typeof globalThis.fetch>();
const json = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });

beforeEach(() => {
  vi.stubEnv("BACKEND_URL", "http://backend:8080");
  vi.stubGlobal("fetch", fetch);
  fetch.mockReset();
});

afterEach(() => {
  vi.unstubAllEnvs();
  vi.unstubAllGlobals();
});

describe("getProductTypes", () => {
  test("asks for 24 product types per page with the search", async () => {
    const page = { items: [], page: 1, size: 24, total: 30 };
    fetch.mockResolvedValue(json(200, page));
    expect(await getProductTypes({ q: "sun", page: 1 })).toEqual(page);
    expect(fetch.mock.calls[0][0]).toBe("http://backend:8080/api/product-types?q=sun&page=1&size=24");
  });

  test("a missing list is an outage, not an empty shop", async () => {
    fetch.mockResolvedValue(json(404, {}));
    await expect(getProductTypes({})).rejects.toThrow("Backend returned 404 for /api/product-types");
  });
});

describe("getProductType", () => {
  test("reads one product type by its encoded code", async () => {
    fetch.mockResolvedValue(json(200, { code: "PT000001" }));
    expect(await getProductType("PT 1")).toEqual({ code: "PT000001" });
    expect(fetch.mock.calls[0][0]).toBe("http://backend:8080/api/product-types/PT%201");
  });

  test("is null when the code does not exist", async () => {
    fetch.mockResolvedValue(json(404, {}));
    expect(await getProductType("PT999999")).toBeNull();
  });
});
