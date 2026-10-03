import { test, expect } from "@playwright/test";
import path from "node:path";
import { readFile } from "node:fs/promises";

async function enterAs(page: import("@playwright/test").Page, role: string) {
  page.on("pageerror", (error) => console.log(`PAGE ERROR: ${error.stack ?? error.message}`));
  await page.goto("/login");
  await page.waitForLoadState("networkidle");
  await page.getByRole("button", { name: new RegExp(role) }).click();
}

test("lecturer sees assigned classes and unsigned PDF is not accepted as valid", async ({ page }) => {
  await enterAs(page, "Giảng viên");
  await expect(page.getByRole("heading", { name: "Bảng điểm của tôi" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Lớp học phần được giao" })).toBeVisible();
  await page.getByLabel("Lớp học phần").selectOption({ label: "CTDL-01 — Cấu trúc dữ liệu và giải thuật" });
  await page.getByLabel("Chọn tệp PDF").setInputFiles({
    name: "unsigned.pdf",
    mimeType: "application/pdf",
    buffer: Buffer.from("%PDF-1.4\n%%EOF\n"),
  });
  const rejectedUpload = page.waitForResponse((response) =>
    new URL(response.url()).pathname === "/api/submissions" && response.request().method() === "POST");
  await page.getByRole("button", { name: "Gửi để xác minh" }).click();
  expect((await rejectedUpload).status()).toBe(422);
  await expect(page.getByRole("alert").filter({ hasText: "NO_SIGNATURE" }))
    .toContainText("PDF chưa được ký số");
  await expect(page.getByRole("status").filter({ hasText: "Đã tiếp nhận" })).toHaveCount(0);
});

test("one lecturer signature creates a submission and notifies the department head", async ({ page }) => {
  await enterAs(page, "Giảng viên");
  await page.getByLabel("Lớp học phần").selectOption({ label: "CTDL-01 — Cấu trúc dữ liệu và giải thuật" });
  await page.getByLabel("Chọn tệp PDF").setInputFiles(path.resolve("../docs/bang-diem-CK_N1.signed.pdf"));
  const upload = page.waitForResponse((response) =>
    new URL(response.url()).pathname === "/api/submissions" && response.request().method() === "POST");
  await page.getByRole("button", { name: "Gửi để xác minh" }).click();
  const response = await upload;
  expect(response.status()).toBe(201);
  const saved = await response.json();
  expect(saved.status).toBe("LECTURER_SIGNED");
  expect(saved.signatureCount).toBe(1);
  await expect(page.getByRole("status").filter({ hasText: "Đã tiếp nhận" })).toContainText("chờ trưởng khoa duyệt");
  await page.getByRole("button", { name: "Đăng xuất" }).click();
  await enterAs(page, "Trưởng khoa");
  await expect(page.getByRole("button", { name: "CTDL-01", exact: true }).first()).toBeVisible();
  const headers = { "X-Demo-Role": "DEPT_HEAD" };
  const queue = await page.request.get("/api/approvals/queue", { headers });
  expect((await queue.json()).some((item: { id: string }) => item.id === saved.id)).toBe(true);
  const notifications = await page.request.get("/api/notifications", { headers });
  expect(JSON.stringify(await notifications.json())).toContain(saved.id);
  // A department head cannot archive the same PDF before adding a second signature.
  const approval = await page.request.post(`/api/approvals/${saved.id}/signed-file`, {
    headers,
    multipart: { file: { name: "lecturer-only.pdf", mimeType: "application/pdf",
      buffer: await readFile(path.resolve("../docs/bang-diem-CK_N1.signed.pdf")) } },
  });
  expect(approval.status()).toBe(422);
});

test("department and examination roles see only their own workspace", async ({ page }) => {
  await enterAs(page, "Trưởng khoa");
  await expect(page.getByRole("heading", { name: "Hồ sơ chờ duyệt" })).toBeVisible();
  await page.getByRole("button", { name: "Đăng xuất" }).click();
  await enterAs(page, "Khảo thí");
  await expect(page.getByRole("heading", { name: "Tra cứu bảng điểm" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Tra cứu" })).toBeVisible();
});
