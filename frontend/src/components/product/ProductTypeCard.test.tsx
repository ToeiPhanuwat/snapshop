import { render, screen } from "@testing-library/react";
import { describe, expect, test } from "vitest";
import { ProductTypeCard } from "./ProductTypeCard";

describe("ProductTypeCard", () => {
  test("links to the product type with its name, shop count and price range", () => {
    render(
      <ProductTypeCard
        type={{ code: "PT000003", name: "Sunscreen 30ml", offerCount: 4, minPrice: 299, maxPrice: 399, imageUrl: null }}
      />,
    );
    const card = screen.getByRole("link");
    expect(card).toHaveAttribute("href", "/types/PT000003");
    expect(screen.getByRole("heading", { name: "Sunscreen 30ml" })).toBeInTheDocument();
    expect(card).toHaveTextContent("4 ร้าน");
    expect(card).toHaveTextContent("฿299.00 – ฿399.00");
  });
});
