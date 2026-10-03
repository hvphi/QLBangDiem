import { test, expect } from "@playwright/test";

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
  await page.getByRole("button", { name: "Gửi để xác minh" }).click();
  await expect(page.getByRole("alert").filter({ hasText: "Không thể ghi nhận bảng điểm" }))
    .toContainText("Không thể ghi nhận bảng điểm");
});

test("department and examination roles see only their own workspace", async ({ page }) => {
  await enterAs(page, "Trưởng khoa");
  await expect(page.getByRole("heading", { name: "Hồ sơ chờ duyệt" })).toBeVisible();
  await page.getByRole("button", { name: "Đăng xuất" }).click();
  await enterAs(page, "Khảo thí");
  await expect(page.getByRole("heading", { name: "Tra cứu bảng điểm" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Tra cứu" })).toBeVisible();
});
