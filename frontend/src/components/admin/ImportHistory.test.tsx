import { render, screen } from "@testing-library/react";
import { describe, expect, test } from "vitest";
import { importResult } from "@/test/fixtures";
import { ImportHistory } from "./ImportHistory";

describe("ImportHistory", () => {
  test("says nothing was imported yet", () => {
    render(<ImportHistory imports={[]} />);
    expect(screen.getByText("ยังไม่เคยนำเข้า")).toBeInTheDocument();
  });

  test("shows each import's time, file and counts", () => {
    render(
      <ImportHistory
        imports={[
          importResult({
            batchId: 9,
            importedAt: "2026-01-01T17:30:00Z",
            sourceFiles: "drpong_20260101.csv",
            rowCount: 192,
            insertedCount: 12,
            priceChangedCount: 3,
            notInLatestCount: 7,
            newTypeCount: 2,
          }),
        ]}
      />,
    );
    expect(screen.getAllByRole("cell").map((td) => td.textContent)).toEqual([
      "2 ม.ค. 2569 00:30",
      "drpong_20260101.csv",
      "192",
      "12",
      "3",
      "7",
      "2",
    ]);
  });
});
