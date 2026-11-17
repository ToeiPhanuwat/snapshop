import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, test } from "vitest";
import { ProductImage } from "./ProductImage";

describe("ProductImage", () => {
  test("shows the picture", () => {
    render(<ProductImage src="https://cf.shopee.co.th/file/a.jpg" alt="Sunscreen" />);
    expect(screen.getByRole("img", { name: "Sunscreen" })).toHaveAttribute("src", "https://cf.shopee.co.th/file/a.jpg");
    expect(screen.queryByText("ยังไม่มีรูป")).not.toBeInTheDocument();
  });

  test("shows a placeholder when there is no picture", () => {
    render(<ProductImage src={null} alt="Sunscreen" />);
    expect(screen.getByText("ยังไม่มีรูป")).toBeInTheDocument();
    expect(screen.queryByRole("img")).not.toBeInTheDocument();
  });

  test("shows the placeholder instead of a broken picture", () => {
    render(<ProductImage src="https://cf.shopee.co.th/file/gone.jpg" alt="Sunscreen" />);
    fireEvent.error(screen.getByRole("img", { name: "Sunscreen" }));
    expect(screen.getByText("ยังไม่มีรูป")).toBeInTheDocument();
    expect(screen.queryByRole("img")).not.toBeInTheDocument();
  });
});
