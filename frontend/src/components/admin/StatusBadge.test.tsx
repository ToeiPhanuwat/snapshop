import { render, screen } from "@testing-library/react";
import { describe, expect, test } from "vitest";
import type { ProductStatus } from "@/types/api";
import { StatusBadge } from "./StatusBadge";

describe("StatusBadge", () => {
  test.each<[ProductStatus, string]>([
    ["OK", "ปกติ"],
    ["NOT_IN_LATEST", "ไม่อยู่ในรอบล่าสุด"],
    ["TEST", "สินค้าทดสอบ"],
    ["OUT_OF_STOCK", "สินค้าหมด"],
    ["REVIEW", "รอตรวจ"],
    ["PRICE_PENDING", "รอราคาจากฟีด"],
  ])("names %s in Thai", (status, label) => {
    render(<StatusBadge status={status} />);
    expect(screen.getByText(label)).toBeInTheDocument();
  });
});
