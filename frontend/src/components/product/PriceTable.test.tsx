import { render, screen, within } from "@testing-library/react";
import { describe, expect, test } from "vitest";
import { offer } from "@/test/fixtures";
import { PriceTable } from "./PriceTable";

describe("PriceTable", () => {
  const offers = [
    offer({ itemId: 1, shopName: "cheap shop", price: 299, shopType: "REGULAR", soldText: "1.2พัน" }),
    offer({ itemId: 2, shopName: "mall shop", price: 399, shopType: "MALL", link: "https://shopee.co.th/product/9/2" }),
  ];

  test("lists each shop's offer in the order given", () => {
    render(<PriceTable offers={offers} />);
    const [first, second] = screen.getAllByRole("listitem");
    expect(first).toHaveTextContent("cheap shop");
    expect(first).toHaveTextContent("ทั่วไป");
    expect(first).toHaveTextContent("ขายแล้ว 1.2พัน");
    expect(first).toHaveTextContent("฿299.00");
    expect(second).toHaveTextContent("mall shop");
    expect(second).toHaveTextContent("Mall");
    expect(second).not.toHaveTextContent("ขายแล้ว");
  });

  test("highlights only the cheapest price", () => {
    render(<PriceTable offers={offers} />);
    expect(screen.getByText("฿299.00")).toHaveClass("text-orange-600");
    expect(screen.getByText("฿399.00")).not.toHaveClass("text-orange-600");
  });

  test("links to the shop in a new tab as a sponsored link", () => {
    render(<PriceTable offers={offers} />);
    const shop = within(screen.getAllByRole("listitem")[1]).getByRole("link", { name: "ไปที่ร้าน" });
    expect(shop).toHaveAttribute("href", "https://shopee.co.th/product/9/2");
    expect(shop).toHaveAttribute("target", "_blank");
    expect(shop).toHaveAttribute("rel", "noopener noreferrer sponsored");
  });
});
