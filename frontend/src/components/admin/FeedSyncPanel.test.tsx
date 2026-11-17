import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, test, vi } from "vitest";
import { retryFeedAction, type ActionState } from "@/app/admin/actions";
import { deferred, feedRun } from "@/test/fixtures";
import { FeedSyncPanel } from "./FeedSyncPanel";

vi.mock("@/app/admin/actions", () => ({ retryFeedAction: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => ({ refresh: vi.fn() }) }));

const retry = vi.mocked(retryFeedAction);
const button = () => screen.getByRole("button", { name: /ดึงรายการที่ค้างจากฟีด|กำลังส่งคำขอ/ });

beforeEach(() => {
  retry.mockReset();
});

describe("FeedSyncPanel", () => {
  test("says no look-up has run yet", () => {
    render(<FeedSyncPanel runs={[]} />);
    expect(screen.getByText("ยังไม่เคยดึงจากฟีด")).toBeInTheDocument();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });

  test("the button works when nothing is running", () => {
    render(<FeedSyncPanel runs={[feedRun({ outcome: "FAILED" })]} />);
    expect(button()).toBeEnabled();
  });

  test("the button stays disabled while a look-up runs", () => {
    render(<FeedSyncPanel runs={[feedRun({ runId: 2, outcome: "RUNNING" }), feedRun({ runId: 1, outcome: "DONE" })]} />);
    expect(button()).toBeDisabled();
  });

  test("shows each run's outcome, counts and note", () => {
    render(
      <FeedSyncPanel runs={[feedRun({ outcome: "RATE_LIMITED", requestedCount: 40, foundCount: 12, message: "โดน 429" })]} />,
    );
    const row = screen.getAllByRole("row")[1];
    expect(within(row).getByText("ถูกจำกัด (429)")).toBeInTheDocument();
    expect(within(row).getByText("40")).toBeInTheDocument();
    expect(within(row).getByText("12")).toBeInTheDocument();
    expect(within(row).getByText("โดน 429")).toBeInTheDocument();
  });

  test("lists only the five latest runs", () => {
    const runs = Array.from({ length: 7 }, (_, i) => feedRun({ runId: 7 - i, message: `run ${7 - i}` }));
    render(<FeedSyncPanel runs={runs} />);
    expect(screen.getAllByRole("row")).toHaveLength(1 + 5);
    expect(screen.getByText("run 3")).toBeInTheDocument();
    expect(screen.queryByText("run 2")).not.toBeInTheDocument();
  });

  test("disables the button while the request is sent, then shows the answer", async () => {
    const answer = deferred<ActionState>();
    retry.mockReturnValue(answer.promise);
    render(<FeedSyncPanel runs={[]} />);

    await userEvent.click(button());
    expect(button()).toBeDisabled();
    expect(button()).toHaveTextContent("กำลังส่งคำขอ…");

    answer.resolve({ ok: false, message: "มีการดึงอยู่แล้ว" });
    expect(await screen.findByText("มีการดึงอยู่แล้ว")).toBeInTheDocument();
    expect(button()).toBeEnabled();
    expect(retry).toHaveBeenCalledTimes(1);
  });
});
