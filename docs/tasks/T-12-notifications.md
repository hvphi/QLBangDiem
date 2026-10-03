---
id: T-12
title: Xây dựng thông báo quy trình
status: in_progress
model: codex
effort: medium
depends_on: ["T-09", "T-10"]
touches:
  - "backend/core/src/main/java/app/webbangdiem/notification/**"
  - "backend/core/src/test/java/app/webbangdiem/notification/**"
  - "backend/contracts/notification/**"
prd_refs: ["§8.3"]
owner: null
started_at: 2026-09-28
finished_at: null
---

# T-12 · Xây dựng thông báo quy trình

## Mục tiêu
Thông báo trưởng khoa khi có bảng điểm chờ và giảng viên khi hồ sơ được duyệt/từ chối; lưu inbox portal.

## Ngữ cảnh cần biết
Sequence PRD có notification file chờ duyệt sau khi chữ ký GV hợp lệ. Kênh cụ thể theo T-01; inbox portal là mức tối thiểu.

## Phạm vi
**Trong:** Event consumer, notification persistence, mark-read API và adapter kênh ngoài nếu được chốt.

**Ngoài:** Sửa event producer, UI cụ thể (T-15/T-16/T-17), email chứa PDF/điểm.

## Đầu vào đã có
- Workflow events T-09/T-10 và schema T-03.
- Auth/scope T-04.
- Kênh/throttle policy trong quyết định T-01.

## Việc phải làm
1. Consume event upload hợp lệ, approval và rejection.
2. Tạo notification đúng recipient theo lớp/khoa.
3. Đảm bảo event retry không tạo notification trùng.
4. Cung cấp API list/mark-read phân trang, có authz.
5. Nếu có kênh ngoài, chỉ gửi link an toàn và không lộ thông tin điểm.

## Quy ước bắt buộc
- Chỉ sửa đường dẫn trong touches của card đang làm. Không sửa card khác để đổi phụ thuộc/trạng thái.
- Không hard-code bí mật, URL CA/OCSP/CRL/TSA, issuer OIDC, thông tin MinIO/NAS hoặc Google Drive; dùng cấu hình môi trường được tài liệu hóa.
- Chỉ sở hữu notification module/contract trong touches.
- Dùng event contract đã công bố; không sửa producer khác.
- Không log nội dung nhạy cảm.

## Checklist đầu ra
- [ ] Backend compile/test theo CONVENTIONS.md.
- [ ] Event/API tests xanh cho recipient, duplicate và scope.
- [ ] Không gửi thông tin nhạy cảm qua kênh ngoài.
- [ ] Không đụng file ngoài touches.
- [ ] Cập nhật status: review và finished_at trong frontmatter card này.
- [ ] Ghi 3–5 dòng “Đã làm gì” vào cuối card.

## Test phải viết
- Upload hợp lệ báo đúng trưởng khoa.
- Approve/reject báo đúng giảng viên.
- Event lặp không tạo notification trùng.
- User không xem/mark-read thông báo người khác.

## Định nghĩa "xong"
Người liên quan nhận notification đúng lúc trong portal và không đọc được inbox ngoài quyền.

## Cạm bẫy đã biết
Không đưa tên sinh viên hoặc điểm vào email/log nếu không cần.

## Đã làm gì
(agent điền khi xong)

