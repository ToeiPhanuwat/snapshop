import { render, screen } from "@testing-library/react";
import { describe, expect, test } from "vitest";
import { Pagination } from "./Pagination";

const nav = () => screen.queryByRole("navigation", { name: "เปลี่ยนหน้า" });
const link = (name: string) => screen.queryByRole("link", { name });

describe("Pagination", () => {
  test.each([0, 24])("is hidden when everything fits on one page (%i items)", (total) => {
    render(<Pagination path="/" params={{}} page={0} size={24} total={total} />);
    expect(nav()).not.toBeInTheDocument();
  });

  test("the first page links only forward and keeps the search", () => {
    render(<Pagination path="/" params={{ q: "ครีม" }} page={0} size={24} total={25} />);
    expect(nav()).toHaveTextContent("หน้า 1 / 2");
    expect(link("ก่อนหน้า")).not.toBeInTheDocument();
    const next = new URL(link("ถัดไป")!.getAttribute("href")!, "http://x");
    expect(next.pathname).toBe("/");
    expect(next.searchParams.get("q")).toBe("ครีม");
    expect(next.searchParams.get("page")).toBe("1");
  });

  test("the last page links only back, and page 0 is left out of the address", () => {
    render(<Pagination path="/admin/types" params={{ q: undefined }} page={1} size={24} total={48} />);
    expect(nav()).toHaveTextContent("หน้า 2 / 2");
    expect(link("ก่อนหน้า")).toHaveAttribute("href", "/admin/types");
    expect(link("ถัดไป")).not.toBeInTheDocument();
  });

  test("a middle page links both ways and keeps the other filters", () => {
    render(<Pagination path="/admin/products" params={{ status: "REVIEW" }} page={2} size={10} total={50} />);
    expect(link("ก่อนหน้า")).toHaveAttribute("href", "/admin/products?status=REVIEW&page=1");
    expect(link("ถัดไป")).toHaveAttribute("href", "/admin/products?status=REVIEW&page=3");
  });
});
