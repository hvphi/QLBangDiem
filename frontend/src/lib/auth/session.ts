export type DemoRole = "LECTURER" | "DEPT_HEAD" | "EXAMINATION";
export type AppRole = DemoRole | "ADMIN";

const key = "qlbd.demo-role";

export function getDemoRole(): DemoRole | null {
  if (typeof window === "undefined") return null;
  const value = window.localStorage.getItem(key);
  return value === "LECTURER" || value === "DEPT_HEAD" || value === "EXAMINATION" ? value : null;
}

export function setDemoRole(role: DemoRole) {
  window.localStorage.setItem(key, role);
}

export function clearDemoRole() {
  window.localStorage.removeItem(key);
}

export function roleLabel(role: AppRole) {
  return role === "LECTURER" ? "Giảng viên" : role === "DEPT_HEAD" ? "Trưởng khoa" : role === "ADMIN" ? "Quản trị" : "Khảo thí";
}

export function roleHome(role: AppRole) {
  return role === "LECTURER" ? "/lecturer" : role === "DEPT_HEAD" ? "/department" : "/examination";
}
