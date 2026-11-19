import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, test, vi } from "vitest";
import { confirmRuleGroupAction, type ActionState } from "@/app/admin/actions";
import { deferred, ruleGroup } from "@/test/fixtures";
import { RuleGroupReviewCard } from "./RuleGroupReviewCard";

vi.mock("@/app/admin/actions", () => ({ confirmRuleGroupAction: vi.fn() }));

const confirm = vi.mocked(confirmRuleGroupAction);
const checkbox = (code: string) => screen.getByRole("checkbox", { name: `รวม ${code}` });

beforeEach(() => {
  confirm.mockReset();
});

describe("RuleGroupReviewCard", () => {
  test("shows the rule group with each name group and which items belong to the rule", () => {
    render(<RuleGroupReviewCard group={ruleGroup()} />);

    expect(screen.getByRole("heading", { name: "Calmmea B5 Aqua Gel" })).toBeInTheDocument();
    expect(screen.getByText("รอตรวจ 2 รายการ")).toBeInTheDocument();
    const first = screen.getByRole("group", { name: "Aqua Gel 10 g" });
    const items = within(first).getAllByRole("listitem");
    expect(items[0]).toHaveTextContent("อยู่ในกลุ่ม R");
    expect(items[1]).not.toHaveTextContent("อยู่ในกลุ่ม R");
    expect(within(screen.getByRole("group", { name: "Aqua Gel x6" })).getByRole("listitem")).toHaveTextContent(
      "ไม่ทราบราคา",
    );
  });

  test("a rule group with nothing waiting shows no waiting count", () => {
    render(<RuleGroupReviewCard group={ruleGroup({ pendingCount: 0 })} />);
    expect(screen.queryByText(/รอตรวจ \d+ รายการ/)).not.toBeInTheDocument();
  });

  test("ticks every name group by default", () => {
    render(<RuleGroupReviewCard group={ruleGroup()} />);
    expect(checkbox("PT000011")).toBeChecked();
    expect(checkbox("PT000012")).toBeChecked();
  });

  test("a name group already merged cannot be ticked, and says where it went", () => {
    const group = ruleGroup();
    group.nameGroups[0].mergedIntoCode = "PT000009";
    group.nameGroups[1].mergedIntoCode = "PT000003";
    render(<RuleGroupReviewCard group={group} />);

    expect(checkbox("PT000011")).toBeDisabled();
    expect(checkbox("PT000011")).not.toBeChecked();
    expect(screen.getByText("รวมเข้า PT000009 แล้ว")).toBeInTheDocument();
    expect(checkbox("PT000012")).toBeDisabled();
    expect(screen.getByText("รวมเข้ากลุ่มนี้แล้ว")).toBeInTheDocument();
  });

  test("sends the rule group and the ticked name groups, and keeps the button disabled until done", async () => {
    const answer = deferred<ActionState>();
    confirm.mockReturnValue(answer.promise);
    render(<RuleGroupReviewCard group={ruleGroup()} />);

    await userEvent.click(checkbox("PT000011"));
    await userEvent.click(screen.getByRole("button", { name: "ยืนยัน" }));
    const sent = confirm.mock.calls[0][1];
    expect(sent.get("code")).toBe("PT000003");
    expect(sent.getAll("merge")).toEqual(["PT000012"]);
    expect(screen.getByRole("button", { name: "กำลังยืนยัน…" })).toBeDisabled();

    answer.resolve({ ok: true, message: "ยืนยัน PT000003 แล้ว: รวม 1 กลุ่ม" });
    expect(await screen.findByText("ยืนยัน PT000003 แล้ว: รวม 1 กลุ่ม")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "ยืนยัน" })).toBeEnabled();
  });

  test("shows why the backend refused", async () => {
    confirm.mockResolvedValue({ ok: false, message: "กลุ่ม PT000011 รวมเข้ากลุ่ม R อื่นแล้ว" });
    render(<RuleGroupReviewCard group={ruleGroup()} />);

    await userEvent.click(screen.getByRole("button", { name: "ยืนยัน" }));
    expect(await screen.findByRole("status")).toHaveTextContent("กลุ่ม PT000011 รวมเข้ากลุ่ม R อื่นแล้ว");
  });
});
