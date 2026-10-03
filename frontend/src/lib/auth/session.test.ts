import { describe, expect, it } from "vitest";
import { roleHome, roleLabel } from "./session";

describe("role navigation", () => {
  it.each([
    ["LECTURER", "/lecturer", "Giảng viên"],
    ["DEPT_HEAD", "/department", "Trưởng khoa"],
    ["EXAMINATION", "/examination", "Khảo thí"],
    ["ADMIN", "/examination", "Quản trị"],
  ] as const)("maps %s to its permitted workspace", (role, home, label) => {
    expect(roleHome(role)).toBe(home);
    expect(roleLabel(role)).toBe(label);
  });
});
