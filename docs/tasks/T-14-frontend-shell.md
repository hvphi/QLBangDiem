---
id: T-14
title: Tạo khung giao diện và đăng nhập
status: in_progress
model: codex
effort: medium
depends_on: ["T-02", "T-04"]
touches:
  - "frontend/app/layout.tsx"
  - "frontend/app/login/**"
  - "frontend/src/components/shell/**"
  - "frontend/src/lib/auth/**"
  - "frontend/src/styles/globals.css"
prd_refs: ["§8.1", "§8.2"]
owner: null
started_at: 2026-09-28
finished_at: null
---

# T-14 · Tạo khung giao diện và đăng nhập

## Mục tiêu
Tạo app shell, điều hướng theo vai trò, trạng thái đăng nhập và xử lý lỗi/session hết hạn dùng chung cho mọi portal.

## Ngữ cảnh cần biết
PRD yêu cầu portal giảng viên, trưởng khoa và Khảo thí. Auth backend do T-04 cung cấp; UI không thay lớp bảo vệ.

## Phạm vi
**Trong:** Layout, menu, auth callback/session, loading/error, accessibility và design tokens dùng chung.

**Ngoài:** Route nghiệp vụ T-15/T-16/T-17, backend authz, branding chưa cung cấp.

## Đầu vào đã có
- Frontend skeleton/design decision T-01/T-02.
- Auth API/claims T-04.

## Việc phải làm
1. Tạo responsive layout và navigation phù hợp vai trò.
2. Tích hợp login/logout theo OIDC/token flow đã chốt; bảo vệ token theo policy.
3. Xử lý session hết hạn, network/access errors mà không lộ data trước khi auth xong.
4. Tạo shared shell components và contract props.
5. Đảm bảo keyboard/focus/labels và viewport di động.

## Quy ước bắt buộc
- Chỉ sửa đường dẫn trong touches của card đang làm. Không sửa card khác để đổi phụ thuộc/trạng thái.
- Không hard-code bí mật, URL CA/OCSP/CRL/TSA, issuer OIDC, thông tin MinIO/NAS hoặc Google Drive; dùng cấu hình môi trường được tài liệu hóa.
- T-14 là chủ duy nhất của layout/auth UI/shared shell.
- Feature cards dùng shared component; yêu cầu mở rộng qua T-14.
- Backend vẫn kiểm tra mọi quyền.

## Checklist đầu ra
- [ ] npm typecheck và component tests theo CONVENTIONS.md xanh.
- [ ] E2E login/logout/session timeout/role navigation xanh.
- [ ] Shell hoạt động ở màn hình nhỏ và bằng bàn phím.
- [ ] Không đụng file ngoài touches.
- [ ] Cập nhật status: review và finished_at trong frontmatter card này.
- [ ] Ghi 3–5 dòng “Đã làm gì” vào cuối card.

## Test phải viết
- Người chưa login được chuyển tới login.
- Login hiển thị role phù hợp; logout xóa session theo policy.
- Session hết hạn không giữ dữ liệu đang mở.
- Menu ẩn route ngoài role và access denied có thông báo.
- Shell dùng được trên viewport mobile.

## Định nghĩa "xong"
Portal gắn route riêng vào shared shell và dùng auth state/menu/error session thống nhất.

## Cạm bẫy đã biết
Ẩn menu không ngăn truy cập API; không render dữ liệu nhạy cảm trong lúc khôi phục session.

## Đã làm gì
(agent điền khi xong)

