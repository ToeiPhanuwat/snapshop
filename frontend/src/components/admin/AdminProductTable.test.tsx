import { render, screen, within } from "@testing-library/react";
import { describe, expect, test, vi } from "vitest";
import { adminProduct } from "@/test/fixtures";
import { AdminProductTable } from "./AdminProductTable";

vi.mock("@/app/admin/actions", () => ({ resolveAction: vi.fn(), moveAction: vi.fn(), deactivateAction: vi.fn() }));

describe("AdminProductTable", () => {
  test("says there is nothing to show", () => {
    render(<AdminProductTable products={[]} />);
    expect(screen.getByText("ไม่มีรายการ")).toBeInTheDocument();
  });

  test("shows what the admin needs to decide on each item", () => {
    render(
      <AdminProductTable
        products={[
          adminProduct({
            itemId: 1001,
            displayName: "Dr.PONG Sunscreen",
            status: "REVIEW",
            onUserSite: false,
            shopType: "PREFERRED",
            shopName: "beauty shop",
            price: null,
            productTypeCode: "PT000007",
            typeName: "Sunscreen 30ml",
            link: "https://shopee.co.th/product/1/1001",
          }),
        ]}
      />,
    );
    const item = screen.getAllByRole("listitem")[0];
    expect(within(item).getByText("Dr.PONG Sunscreen")).toBeInTheDocument();
    expect(within(item).getByText("รอตรวจ")).toBeInTheDocument();
    expect(within(item).getByText("ไม่แสดงฝั่ง user")).toBeInTheDocument();
    expect(within(item).getByText("ร้านแนะนำ")).toBeInTheDocument();
    expect(within(item).getByText("beauty shop")).toBeInTheDocument();
    expect(within(item).getByText("· ไม่ทราบราคา")).toBeInTheDocument();
    expect(within(item).getByText("· item 1001")).toBeInTheDocument();
    expect(within(item).getByRole("link", { name: "PT000007" })).toHaveAttribute(
      "href",
      "/admin/products?typeCode=PT000007",
    );
    const shop = within(item).getByRole("link", { name: "เปิดหน้าสินค้า" });
    expect(shop).toHaveAttribute("href", "https://shopee.co.th/product/1/1001");
    expect(shop).toHaveAttribute("target", "_blank");
  });

  test("marks an item that the shoppers see", () => {
    render(<AdminProductTable products={[adminProduct({ onUserSite: true })]} />);
    expect(screen.getByText("แสดงฝั่ง user")).toBeInTheDocument();
  });
});
