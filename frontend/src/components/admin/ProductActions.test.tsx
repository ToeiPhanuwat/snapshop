import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { deactivateAction, moveAction, resolveAction, type ActionState } from "@/app/admin/actions";
import { adminProduct, deferred } from "@/test/fixtures";
import { ProductActions } from "./ProductActions";

vi.mock("@/app/admin/actions", () => ({ resolveAction: vi.fn(), moveAction: vi.fn(), deactivateAction: vi.fn() }));

const resolve = vi.mocked(resolveAction);
const move = vi.mocked(moveAction);
const deactivate = vi.mocked(deactivateAction);
const button = (name: string) => screen.getByRole("button", { name });
const buttons = () => screen.getAllByRole("button");

beforeEach(() => {
  resolve.mockReset();
  move.mockReset();
  deactivate.mockReset();
  vi.spyOn(window, "confirm").mockReturnValue(true);
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe("ProductActions", () => {
  test("offers the review button only for an item waiting for review", () => {
    const { rerender } = render(<ProductActions product={adminProduct({ status: "REVIEW" })} />);
    expect(button("ตรวจแล้ว")).toBeInTheDocument();
    rerender(<ProductActions product={adminProduct({ status: "OK" })} />);
    expect(screen.queryByRole("button", { name: "ตรวจแล้ว" })).not.toBeInTheDocument();
  });

  test.each([
    ["ตรวจแล้ว", resolve],
    ["ย้ายกลุ่ม", move],
    ["เลิกขาย", deactivate],
  ] as const)("disables every button while %s runs", async (name, action) => {
    const answer = deferred<ActionState>();
    action.mockReturnValue(answer.promise);
    render(<ProductActions product={adminProduct({ itemId: 1001, status: "REVIEW" })} />);

    await userEvent.click(button(name));
    expect(action.mock.calls[0][1].get("itemId")).toBe("1001");
    for (const b of buttons()) {
      expect(b).toBeDisabled();
    }

    answer.resolve({ ok: true, message: "เสร็จแล้ว" });
    expect(await screen.findByText("เสร็จแล้ว")).toBeInTheDocument();
    for (const b of buttons()) {
      expect(b).toBeEnabled();
    }
  });

  test("sends the group code the admin typed", async () => {
    move.mockResolvedValue({ ok: true, message: "ย้ายไป PT000002 แล้ว" });
    render(<ProductActions product={adminProduct()} />);

    await userEvent.type(screen.getByRole("textbox", { name: "ย้ายไปกลุ่ม" }), "pt000002");
    await userEvent.click(button("ย้ายกลุ่ม"));
    expect(move.mock.calls[0][1].get("productTypeCode")).toBe("pt000002");
    expect(await screen.findByText("ย้ายไป PT000002 แล้ว")).toBeInTheDocument();
  });

  test("asks before marking an item discontinued, and does nothing on cancel", async () => {
    vi.mocked(window.confirm).mockReturnValue(false);
    render(<ProductActions product={adminProduct({ itemId: 1001 })} />);

    await userEvent.click(button("เลิกขาย"));
    expect(window.confirm).toHaveBeenCalledWith(expect.stringContaining("1001"));
    expect(deactivate).not.toHaveBeenCalled();
  });
});
