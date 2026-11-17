// @vitest-environment node
import { afterEach, describe, expect, test, vi } from "vitest";
import { backendUrl, query } from "./http";

afterEach(() => {
  vi.unstubAllEnvs();
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
