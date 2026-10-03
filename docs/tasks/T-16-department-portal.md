---
id: T-16
title: Xây dựng portal trưởng khoa
status: in_progress
model: codex
effort: high
depends_on: ["T-08", "T-10", "T-12", "T-14"]
touches:
  - "frontend/app/department/**"
  - "frontend/src/features/department/**"
prd_refs: ["§8.1", "§8.3"]
owner: null
started_at: 2026-09-28
finished_at: null
---

# T-16 · Xây dựng portal trưởng khoa

## Mục tiêu
Cho trưởng khoa xem hàng chờ trong phạm vi khoa, mở PDF/metadata đã xác minh, ký duyệt cấp 2 hoặc từ chối có lý do.

## Ngữ cảnh cần biết
PRD yêu cầu thông báo hồ sơ chờ, trưởng khoa ký cấp 2, hệ thống xác thực cả hai chữ ký rồi lưu trữ.

## Phạm vi
**Trong:** Review queue, transcript detail, signing bridge, approve/reject và notification result.

**Ngoài:** Sửa PDF/điểm, bỏ qua server verification, quản trị catalog, shared shell.

## Đầu vào đã có
- Approval API T-10; notification API T-12; bridge T-08; shell T-14.
- Scope/policy T-04/T-01.

## Việc phải làm
1. Hiển thị queue theo khoa/kỳ/mã lớp/giảng viên/status.
2. Mở PDF read-only cùng metadata chữ ký và verify warning.
3. Ký bằng bridge hoặc upload signed PDF, yêu cầu xác nhận hành động.
4. Reject yêu cầu lý do theo policy đã chốt.
5. Hiển thị lưu trữ/notification chỉ sau xác nhận API.

## Quy ước bắt buộc
- Chỉ sửa đường dẫn trong touches của card đang làm. Không sửa card khác để đổi phụ thuộc/trạng thái.
- Không hard-code bí mật, URL CA/OCSP/CRL/TSA, issuer OIDC, thông tin MinIO/NAS hoặc Google Drive; dùng cấu hình môi trường được tài liệu hóa.
- Chỉ sửa route/feature department.
- Dùng shared shell; server luôn authorize/verify.
- Không cho sửa nội dung PDF/bảng điểm.

## Checklist đầu ra
- [ ] Frontend typecheck/tests theo CONVENTIONS.md xanh.
- [ ] E2E approve/reject/permission xanh.
- [ ] Không đụng file ngoài touches.
- [ ] Không đụng file ngoài touches.
- [ ] Cập nhật status: review và finished_at trong frontmatter card này.
- [ ] Ghi 3–5 dòng “Đã làm gì” vào cuối card.

## Test phải viết
- Trưởng khoa chỉ thấy queue đúng khoa.
- Hai signature hợp lệ cho phép approve.
- Verify lỗi chặn approve và hiện reason.
- Reject cần lý do và báo đúng giảng viên.
- Giảng viên không gọi được route/action trưởng khoa.

## Định nghĩa "xong"
Trưởng khoa review/approve/reject an toàn từ queue đến kết quả.

## Cạm bẫy đã biết
Không ký bản cũ trong tab nếu transcript revision đã đổi; kiểm tra version trước submit.

## Đã làm gì
(agent điền khi xong)

