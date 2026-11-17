import { act, fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, test, vi } from "vitest";
import { importAction, type ActionState } from "@/app/admin/actions";
import { deferred, importResult } from "@/test/fixtures";
import { ImportForm } from "./ImportForm";

vi.mock("@/app/admin/actions", () => ({ importAction: vi.fn() }));

const importFile = vi.mocked(importAction);
const csv = new File(["รหัสสินค้า\n1"], "drpong.csv", { type: "text/csv" });

beforeEach(() => {
  importFile.mockReset();
});

async function submit() {
  await userEvent.upload(screen.getByLabelText(/ไฟล์ CSV/), csv);
  // jsdom never counts a chosen file as filling a `required` file input, so a click would stop at validation;
  // a browser would submit here, so the test sends the submit event itself.
  await act(async () => {
    fireEvent.submit(screen.getByRole("button").closest("form")!);
  });
}

describe("ImportForm", () => {
  test("sends the chosen file and keeps the button disabled until the import is done", async () => {
    const answer = deferred<ActionState>();
    importFile.mockReturnValue(answer.promise);
    render(<ImportForm />);

    await submit();
    expect(screen.getByRole("button")).toBeDisabled();
    expect(screen.getByRole("button")).toHaveTextContent("กำลังนำเข้า…");
    // jsdom leaves the chosen file out of the form data, so that the file really travels is checked end to end.
    expect(importFile).toHaveBeenCalledTimes(1);

    answer.resolve({ ok: true, message: "นำเข้าสำเร็จ", result: importResult() });
    expect(await screen.findByText("นำเข้าสำเร็จ")).toBeInTheDocument();
    expect(screen.getByRole("button")).toBeEnabled();
  });

  test("shows the import's counts", async () => {
    importFile.mockResolvedValue({
      ok: true,
      message: "นำเข้าสำเร็จ",
      result: importResult({
        rowCount: 192,
        insertedCount: 12,
        priceChangedCount: 3,
        unchangedCount: 170,
        notInLatestCount: 7,
        newTypeCount: 2,
      }),
    });
    render(<ImportForm />);
    await submit();

    const counts = await screen.findAllByRole("definition");
    expect(counts.map((dd) => dd.textContent)).toEqual(["192", "12", "3", "170", "7", "2"]);
  });

  test("shows why a file was rejected and no counts", async () => {
    importFile.mockResolvedValue({ ok: false, message: "ไฟล์มีแถวผิด", errors: ["แถว 3: ไม่มีราคา"] });
    render(<ImportForm />);
    await submit();

    expect(await screen.findByText("แถว 3: ไม่มีราคา")).toBeInTheDocument();
    expect(screen.queryAllByRole("definition")).toHaveLength(0);
  });
});
