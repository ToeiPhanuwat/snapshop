import { render, screen } from "@testing-library/react";
import { describe, expect, test } from "vitest";
import { importPreview } from "@/test/fixtures";
import { ImportPreviewView } from "./ImportPreviewView";

describe("ImportPreviewView", () => {
  test("shows every count of what the import would change", () => {
    render(
      <ImportPreviewView
        preview={importPreview({
          rowCount: 192,
          insertedCount: 12,
          priceChangedCount: 3,
          unchangedCount: 170,
          notInLatestCount: 7,
          newTypeCount: 2,
          feedItemCount: 15,
          ruleReviewCount: 89,
          autoPassedCount: 7,
        })}
      />,
    );
    expect(screen.getAllByRole("definition").map((dd) => dd.textContent)).toEqual([
      "192",
      "12",
      "3",
      "170",
      "7",
      "2",
      "15",
      "89",
      "7",
    ]);
  });

  test("says a section is empty", () => {
    render(<ImportPreviewView preview={importPreview()} />);
    expect(screen.getByText("ราคาเปลี่ยน (0)")).toBeInTheDocument();
    expect(screen.getAllByText("ไม่มี")).toHaveLength(5);
  });

  test("shows the old and the new price of each changed item", () => {
    render(
      <ImportPreviewView
        preview={importPreview({ priceChanges: [{ itemId: 1001, name: "Sunscreen", oldPrice: 399, newPrice: 359 }] })}
      />,
    );
    expect(screen.getByText(/Sunscreen/).closest("li")).toHaveTextContent("1001 Sunscreen: ฿399.00 → ฿359.00");
  });

  test("an item without a known old price says so", () => {
    render(
      <ImportPreviewView
        preview={importPreview({ priceChanges: [{ itemId: 1002, name: "Serum", oldPrice: null, newPrice: 590 }] })}
      />,
    );
    expect(screen.getByText(/Serum/).closest("li")).toHaveTextContent("ไม่ทราบราคา → ฿590.00");
  });

  test("lists what will be hidden, sent to review and the new groups", () => {
    render(
      <ImportPreviewView
        preview={importPreview({
          toHide: [{ itemId: 2001, name: "Old cream" }],
          toReview: [{ itemId: 3001, name: "New toner" }],
          newTypes: ["Toner 100ml"],
        })}
      />,
    );
    expect(screen.getByText("จะถูกซ่อน (ไม่อยู่ในไฟล์นี้) (1)")).toBeInTheDocument();
    expect(screen.getByText(/Old cream/).closest("li")).toHaveTextContent("2001 Old cream");
    expect(screen.getByText(/New toner/).closest("li")).toHaveTextContent("3001 New toner");
    expect(screen.getByText("Toner 100ml")).toBeInTheDocument();
  });

  test("opens sections of up to 20 lines and keeps longer or empty ones closed", () => {
    const many = Array.from({ length: 21 }, (_, i) => ({ itemId: i, name: `item ${i}` }));
    render(<ImportPreviewView preview={importPreview({ toHide: many, toReview: many.slice(0, 20) })} />);
    const section = (title: RegExp) => screen.getByText(title, { selector: "summary" }).closest("details");
    expect(section(/^จะถูกซ่อน/)).not.toHaveAttribute("open");
    expect(section(/^จะรอตรวจ/)).toHaveAttribute("open");
    expect(section(/^กลุ่มใหม่/)).not.toHaveAttribute("open");
  });
});
