import { render, screen } from "@testing-library/react";
import { describe, expect, test } from "vitest";
import { SearchForm } from "./SearchForm";

describe("SearchForm", () => {
  test("is a plain GET search that keeps the words in the address", () => {
    render(<SearchForm action="/admin/types" defaultValue="toner" placeholder="ค้นหากลุ่ม" />);
    const form = screen.getByRole("search");
    expect(form).toHaveAttribute("action", "/admin/types");
    expect(form).not.toHaveAttribute("method");
    const box = screen.getByRole("searchbox");
    expect(box).toHaveAttribute("name", "q");
    expect(box).toHaveValue("toner");
    expect(box).toHaveAttribute("placeholder", "ค้นหากลุ่ม");
    expect(screen.getByRole("button", { name: "ค้นหา" })).toHaveAttribute("type", "submit");
  });

  test("starts empty without a search", () => {
    render(<SearchForm action="/" placeholder="ค้นหาชื่อสินค้า" />);
    expect(screen.getByRole("searchbox")).toHaveValue("");
  });
});
