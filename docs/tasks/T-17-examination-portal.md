---
id: T-17
title: Xây dựng portal Khảo thí
status: in_progress
model: codex
effort: medium
depends_on: ["T-05", "T-11", "T-12", "T-14"]
touches:
  - "frontend/app/examination/**"
  - "frontend/src/features/examination/**"
prd_refs: ["§8.1", "§8.3", "§8.6"]
owner: null
started_at: 2026-09-28
finished_at: null
---

# T-17 · Xây dựng portal Khảo thí

## Mục tiêu
Cung cấp cho Khảo thí tra cứu hồ sơ, trạng thái chữ ký, audit trail và tải bản lưu trong phạm vi được cấp.

## Ngữ cảnh cần biết
PRD nêu portal Khảo thí và log bất biến phục vụ thanh tra; quyền sửa/phạm vi xem chưa chi tiết nên T-01 chốt read-only.

## Phạm vi
**Trong:** Search/filter, workflow/audit timeline, viewer/download có kiểm soát.

**Ngoài:** Sửa transcript, ký thay, xóa audit, quản trị hạ tầng.

## Đầu vào đã có
- Catalog T-05, audit API T-11, notifications T-12, shell T-14.
- Read-only scope T-01/T-04.

## Việc phải làm
1. Tạo search theo năm học/kỳ/khoa/mã lớp/status.
2. Hiển thị transcript, signer/certificate status, hash và audit timeline.
3. Cho download đúng quyền và ghi download event.
4. Phân biệt UNKNOWN với INVALID/VALID.
5. Thêm pagination, empty/error states và verify warnings.

## Quy ước bắt buộc
- Chỉ sửa đường dẫn trong touches của card đang làm. Không sửa card khác để đổi phụ thuộc/trạng thái.
- Không hard-code bí mật, URL CA/OCSP/CRL/TSA, issuer OIDC, thông tin MinIO/NAS hoặc Google Drive; dùng cấu hình môi trường được tài liệu hóa.
- Chỉ sửa route/feature examination.
- Read-only trừ khi T-01 chỉ định rõ; backend vẫn check authz.
- Không hiển thị secret/token/TSA payload thô.

## Checklist đầu ra
- [ ] Frontend typecheck/tests theo CONVENTIONS.md xanh.
- [ ] E2E lookup/audit/download/permissions xanh.
- [ ] Không đụng file ngoài touches.
- [ ] Không đụng file ngoài touches.
- [ ] Cập nhật status: review và finished_at trong frontmatter card này.
- [ ] Ghi 3–5 dòng “Đã làm gì” vào cuối card.

## Test phải viết
- Filter trả đúng hồ sơ trong scope và phân trang.
- Timeline có upload/verify/approve/lock/download theo server time.
- UNKNOWN không bị hiển thị thành VALID.
- Download ngoài scope bị chặn; download hợp lệ phát audit.
- Khảo thí không sửa/xóa transcript hoặc audit.

## Định nghĩa "xong"
Khảo thí truy vết hồ sơ từ nộp tới archive và tải đúng bản theo quyền.

## Cạm bẫy đã biết
Không dùng client timestamp để sắp audit; dùng thời gian server đã lưu.

## Đã làm gì
(agent điền khi xong)

