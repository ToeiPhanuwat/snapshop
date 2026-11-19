import { render, screen, within } from "@testing-library/react";
import { describe, expect, test, vi } from "vitest";
import { ruleGroup } from "@/test/fixtures";
import { RuleGroupSections } from "./RuleGroupSections";

vi.mock("@/app/admin/actions", () => ({ confirmRuleGroupAction: vi.fn() }));

const section = (name: string) => screen.getByRole("region", { name });

describe("RuleGroupSections", () => {
  test("rule groups with items waiting come first, and those with nothing waiting can still be merged", () => {
    render(
      <RuleGroupSections
        groups={[
          ruleGroup({ code: "PT000003", name: "Calmmea B5 Aqua Gel", pendingCount: 1 }),
          ruleGroup({ code: "PT000009", name: "Other", pendingCount: 0 }),
        ]}
      />,
    );

    expect(within(section("รอ REVIEW")).getByRole("heading", { name: "Calmmea B5 Aqua Gel" })).toBeInTheDocument();
    expect(within(section("รอ REVIEW")).queryByRole("heading", { name: "Other" })).not.toBeInTheDocument();
    expect(within(section("ยุบรวมได้")).getByRole("heading", { name: "Other" })).toBeInTheDocument();
    expect(within(section("ยุบรวมได้")).queryByRole("heading", { name: "Calmmea B5 Aqua Gel" })).not.toBeInTheDocument();
  });

  test("a section with no rule groups says so", () => {
    render(<RuleGroupSections groups={[ruleGroup({ pendingCount: 0 })]} />);

    expect(section("รอ REVIEW")).toHaveTextContent("ไม่มีกลุ่ม R ที่รอตรวจ");
    expect(section("ยุบรวมได้")).not.toHaveTextContent("ไม่มีกลุ่ม R");
  });

  test("with nothing at all, both sections say so", () => {
    render(<RuleGroupSections groups={[]} />);

    expect(section("รอ REVIEW")).toHaveTextContent("ไม่มีกลุ่ม R ที่รอตรวจ");
    expect(section("ยุบรวมได้")).toHaveTextContent("ไม่มีกลุ่ม R ที่ยุบรวมได้");
  });
});
