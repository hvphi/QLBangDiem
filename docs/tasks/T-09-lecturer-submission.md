---
id: T-09
title: Xây dựng luồng nộp bảng điểm giảng viên
status: in_progress
model: codex
effort: high
depends_on: ["T-04", "T-05", "T-06", "T-07", "T-11"]
touches:
  - "backend/core/src/main/java/app/webbangdiem/submission/**"
  - "backend/core/src/test/java/app/webbangdiem/submission/**"
  - "backend/contracts/submission/**"
prd_refs: ["§8.2", "§8.3", "§8.4", "§8.6"]
owner: null
started_at: 2026-09-28
finished_at: null
---

# T-09 · Xây dựng luồng nộp bảng điểm giảng viên

## Mục tiêu
Nhận PDF đã ký cấp giảng viên, xác minh trước khi chấp nhận, gắn metadata lớp chuẩn và đưa hồ sơ vào hàng chờ trưởng khoa.

## Ngữ cảnh cần biết
PRD: giảng viên xuất/ký PDF → upload → tự động validate chữ ký GV → thông báo trưởng khoa. Trạng thái hợp lệ đầu tiên là LECTURER_SIGNED.

## Phạm vi
**Trong:** Upload/status API, auth/deadline, gọi T-06, lưu T-07 và phát audit event qua contract T-11.

**Ngoài:** UI (T-15), ký USB (T-08), duyệt cấp 2 (T-10), tạo bảng điểm từ SIS.

## Đầu vào đã có
- Auth T-04, catalog T-05, verification T-06, storage T-07, audit contract T-11.
- Schema T-03 và re-submit policy T-01.

## Việc phải làm
1. Tạo streaming upload có giới hạn size/type/timeout và auth.
2. Xác minh actor phụ trách lớp/deadline; metadata lấy từ catalog, không tin client.
3. Chỉ sau khi chữ ký GV hợp lệ và hash ổn định mới ghi transcript/status.
4. Xử lý duplicate/idempotency và rejection theo policy T-01.
5. Phát audit event upload/verify/result.

## Quy ước bắt buộc
- Chỉ sửa đường dẫn trong touches của card đang làm. Không sửa card khác để đổi phụ thuộc/trạng thái.
- Không hard-code bí mật, URL CA/OCSP/CRL/TSA, issuer OIDC, thông tin MinIO/NAS hoặc Google Drive; dùng cấu hình môi trường được tài liệu hóa.
- Không lưu file invalid như transcript hợp lệ.
- Không sửa migration/contract/package card khác.
- PDF đã ký không đổi byte; dependency lỗi thì fail closed.

## Checklist đầu ra
- [ ] Backend compile/test theo CONVENTIONS.md.
- [ ] Upload API tests cho success/invalid/permission xanh.
- [ ] Audit event phát ra sau hành động đúng.
- [ ] Không đụng file ngoài touches.
- [ ] Cập nhật status: review và finished_at trong frontmatter card này.
- [ ] Ghi 3–5 dòng “Đã làm gì” vào cuối card.

## Test phải viết
- Giảng viên đúng lớp nộp file hợp lệ tạo LECTURER_SIGNED.
- Sai actor, file type/size hoặc hết hạn bị từ chối.
- Invalid/revoked/tampered không lưu như hợp lệ.
- Retry cùng idempotency key không tạo transcript trùng.

## Định nghĩa "xong"
Chỉ PDF vượt qua quyền, deadline, signature và hash mới vào hàng chờ trưởng khoa.

## Cạm bẫy đã biết
Không dùng filename làm khóa; không tin signer/course metadata do browser gửi.

## Đã làm gì
(agent điền khi xong)

