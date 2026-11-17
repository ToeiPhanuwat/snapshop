// @vitest-environment node
import { describe, expect, test } from "vitest";
import { formatDateTime, formatPrice, formatPriceRange } from "./format";

describe("formatPrice", () => {
  test("shows baht with two decimals and thousands separators", () => {
    expect(formatPrice(1234.5)).toBe("฿1,234.50");
  });

  test("says the price is unknown when there is none", () => {
    expect(formatPrice(null)).toBe("ไม่ทราบราคา");
  });

  test("a zero price is a price, not an unknown one", () => {
    expect(formatPrice(0)).toBe("฿0.00");
  });
});

describe("formatPriceRange", () => {
  test("one price when every shop asks the same", () => {
    expect(formatPriceRange(199, 199)).toBe("฿199.00");
  });

  test("lowest to highest when the shops differ", () => {
    expect(formatPriceRange(199, 250)).toBe("฿199.00 – ฿250.00");
  });
});

describe("formatDateTime", () => {
  test("shows Bangkok time, so a late UTC evening is already the next day", () => {
    expect(formatDateTime("2026-01-01T17:30:00Z")).toBe("2 ม.ค. 2569 00:30");
  });
});
