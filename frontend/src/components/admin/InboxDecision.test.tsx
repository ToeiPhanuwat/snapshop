import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { discardInboxAction, importInboxAction, type ActionState } from "@/app/admin/actions";
import { deferred } from "@/test/fixtures";
import { InboxDecision } from "./InboxDecision";

vi.mock("@/app/admin/actions", () => ({ importInboxAction: vi.fn(), discardInboxAction: vi.fn() }));

const importFile = vi.mocked(importInboxAction);
const discard = vi.mocked(discardInboxAction);
const importButton = () => screen.getByRole("button", { name: /นำเข้า/ });
const discardButton = () => screen.getByRole("button", { name: "ทิ้งไฟล์" });

beforeEach(() => {
  importFile.mockReset();
  discard.mockReset();
  vi.spyOn(window, "confirm").mockReturnValue(true);
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe("InboxDecision", () => {
  test("imports the file by name and disables both buttons until the import is done", async () => {
    const answer = deferred<ActionState>();
    importFile.mockReturnValue(answer.promise);
    render(<InboxDecision name="drpong_20261117.csv" canImport />);

    await userEvent.click(importButton());
    expect(importButton()).toHaveTextContent("กำลังนำเข้า…");
    expect(importButton()).toBeDisabled();
    expect(discardButton()).toBeDisabled();
    expect(importFile.mock.calls[0][1].get("name")).toBe("drpong_20261117.csv");

    answer.resolve({ ok: false, message: "ไฟล์หายไปแล้ว" });
    expect(await screen.findByText("ไฟล์หายไปแล้ว")).toBeInTheDocument();
    expect(importButton()).toBeEnabled();
    expect(discardButton()).toBeEnabled();
  });

  test("disables both buttons until the discard is done", async () => {
    const answer = deferred<ActionState>();
    discard.mockReturnValue(answer.promise);
    render(<InboxDecision name="drpong_20261117.csv" canImport />);

    await userEvent.click(discardButton());
    expect(window.confirm).toHaveBeenCalledWith(expect.stringContaining("drpong_20261117.csv"));
    expect(discardButton()).toBeDisabled();
    expect(importButton()).toBeDisabled();

    answer.resolve({ ok: false, message: "ทิ้งไม่ได้" });
    expect(await screen.findByText("ทิ้งไม่ได้")).toBeInTheDocument();
    expect(importButton()).toBeEnabled();
    expect(discardButton()).toBeEnabled();
  });

  test("keeps the file when the admin cancels the discard", async () => {
    vi.mocked(window.confirm).mockReturnValue(false);
    render(<InboxDecision name="drpong_20261117.csv" canImport />);

    await userEvent.click(discardButton());
    expect(discard).not.toHaveBeenCalled();
    expect(discardButton()).toBeEnabled();
  });

  test("offers only the discard for a file that cannot be imported", () => {
    render(<InboxDecision name="broken.csv" canImport={false} />);
    expect(screen.queryByRole("button", { name: /นำเข้า/ })).not.toBeInTheDocument();
    expect(discardButton()).toBeEnabled();
  });
});
