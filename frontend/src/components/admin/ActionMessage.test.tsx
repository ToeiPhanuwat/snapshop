import { render, screen } from "@testing-library/react";
import { describe, expect, test } from "vitest";
import { ActionMessage } from "./ActionMessage";

describe("ActionMessage", () => {
  test("shows nothing before the action ran", () => {
    const { container } = render(<ActionMessage state={null} />);
    expect(container).toBeEmptyDOMElement();
  });

  test("announces the message", () => {
    render(<ActionMessage state={{ ok: true, message: "ตรวจแล้ว" }} />);
    expect(screen.getByRole("status")).toHaveTextContent("ตรวจแล้ว");
    expect(screen.queryByRole("list")).not.toBeInTheDocument();
  });

  test("lists every error of a rejected file", () => {
    render(<ActionMessage state={{ ok: false, message: "ไฟล์ไม่ถูกต้อง", errors: ["แถว 2: ราคา", "แถว 5: ลิงก์"] }} />);
    expect(screen.getAllByRole("listitem").map((li) => li.textContent)).toEqual(["แถว 2: ราคา", "แถว 5: ลิงก์"]);
  });
});
