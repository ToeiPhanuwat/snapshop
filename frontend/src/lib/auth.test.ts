// @vitest-environment node
import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { basicAuthHeader, isAdminAuthorization } from "./auth";

const basic = (user: string, password: string) => "Basic " + Buffer.from(`${user}:${password}`).toString("base64");

beforeEach(() => {
  vi.stubEnv("ADMIN_USERNAME", "admin");
  vi.stubEnv("ADMIN_PASSWORD", "s3cret");
});

afterEach(() => {
  vi.unstubAllEnvs();
});

describe("isAdminAuthorization", () => {
  test("accepts the admin's login", () => {
    expect(isAdminAuthorization(basic("admin", "s3cret"))).toBe(true);
  });

  test.each([
    ["no header", null],
    ["a wrong password", basic("admin", "wrong")],
    ["a password that only starts right", basic("admin", "s3cre")],
    ["a password with extra characters", basic("admin", "s3cretX")],
    ["a wrong user", basic("root", "s3cret")],
    ["another scheme", "Bearer " + Buffer.from("admin:s3cret").toString("base64")],
    ["the right credentials without the scheme", Buffer.from("admin:s3cret").toString("base64")],
  ])("refuses %s", (_, header) => {
    expect(isAdminAuthorization(header)).toBe(false);
  });
});

describe("basicAuthHeader", () => {
  test("is the Basic header of the configured admin", () => {
    expect(basicAuthHeader()).toBe(basic("admin", "s3cret"));
  });

  test.each(["ADMIN_USERNAME", "ADMIN_PASSWORD"])("refuses to run when %s is not set", (name) => {
    vi.stubEnv(name, "");
    expect(() => basicAuthHeader()).toThrow("ADMIN_USERNAME and ADMIN_PASSWORD must be set");
  });
});
