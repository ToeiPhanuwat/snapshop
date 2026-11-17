import { act, render } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { AutoRefresh } from "./AutoRefresh";

const refresh = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ refresh }) }));

beforeEach(() => {
  vi.useFakeTimers();
  refresh.mockReset();
});

afterEach(() => {
  vi.useRealTimers();
});

const wait = (ms: number) => act(() => vi.advanceTimersByTime(ms));

describe("AutoRefresh", () => {
  test("reloads the page data every minute while active", () => {
    render(<AutoRefresh active />);
    wait(59_999);
    expect(refresh).not.toHaveBeenCalled();
    wait(1);
    expect(refresh).toHaveBeenCalledTimes(1);
    wait(60_000);
    expect(refresh).toHaveBeenCalledTimes(2);
  });

  test("does nothing while inactive", () => {
    render(<AutoRefresh active={false} />);
    wait(180_000);
    expect(refresh).not.toHaveBeenCalled();
  });

  test("stops when it becomes inactive", () => {
    const { rerender } = render(<AutoRefresh active />);
    rerender(<AutoRefresh active={false} />);
    wait(180_000);
    expect(refresh).not.toHaveBeenCalled();
  });

  test("stops when the page is left", () => {
    const { unmount } = render(<AutoRefresh active everyMs={1_000} />);
    unmount();
    wait(10_000);
    expect(refresh).not.toHaveBeenCalled();
  });
});
