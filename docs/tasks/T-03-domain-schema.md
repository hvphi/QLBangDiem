---
id: T-03
title: Tạo schema và migration miền nghiệp vụ
status: in_progress
model: codex
effort: high
depends_on: ["T-02"]
touches:
  - "backend/core/src/main/resources/db/migration/**"
  - "backend/core/src/main/java/app/webbangdiem/domain/**"
prd_refs: ["§8.2", "§8.4", "§8.6"]
owner: null
started_at: 2026-09-28
finished_at: null
---

# T-03 · Tạo schema và migration miền nghiệp vụ

## Mục tiêu
Thiết lập schema dùng chung cho bảng điểm, chữ ký, identity, danh mục học vụ, audit và thông báo để feature không tạo migration chồng lấn.

## Ngữ cảnh cần biết
PRD nêu transcripts và digital_signatures; status gồm LECTURER_SIGNED, DEPT_APPROVED, ARCHIVED, REJECTED. Hash PDF là SHA-256; lưu file path và metadata ký.

## Phạm vi
**Trong:** Versioned migration, entity/domain model dùng chung, khóa ngoại, unique/index/check constraints và test migration.

**Ngoài:** Endpoint, UI, policy xác thực chứng thư, nội dung PDF hoặc dữ liệu người dùng thật.

## Đầu vào đã có
- Backend/PostgreSQL local từ T-02.
- Schema bắt buộc tại docs/PRD.md §8.4.
- Trạng thái và giả định tại docs/architecture/DECISIONS.md.

## Việc phải làm
1. Tạo transcript và digital signature schema gồm serial, issuer, TSA token, validation status/time.
2. Tạo bảng tối thiểu user/role/scope, department, academic year, semester, course class, deadline, notification, append-only audit.
3. Áp dụng UUID, timezone-aware timestamps, index tra cứu và unique chống nộp trùng.
4. Mô hình hóa vòng đời để file đã ký không bị xóa dây chuyền ngoài policy.
5. Viết migration forward-only và test trên database rỗng.

## Quy ước bắt buộc
- Chỉ sửa đường dẫn trong touches của card đang làm. Không sửa card khác để đổi phụ thuộc/trạng thái.
- Không hard-code bí mật, URL CA/OCSP/CRL/TSA, issuer OIDC, thông tin MinIO/NAS hoặc Google Drive; dùng cấu hình môi trường được tài liệu hóa.
- T-03 là chủ duy nhất của migration/domain model dùng chung.
- Card khác không sửa migration; yêu cầu thay đổi phải thêm phụ thuộc T-03.
- Tuân thủ naming/timezone/test convention.

## Checklist đầu ra
- [ ] Backend compile và test migration theo CONVENTIONS.md.
- [ ] Schema biểu diễn transcript, chữ ký, trạng thái và audit theo PRD.
- [ ] Migration mới chạy thành công trên DB rỗng.
- [ ] Không đụng file ngoài touches.
- [ ] Cập nhật status: review và finished_at trong frontmatter card này.
- [ ] Ghi 3–5 dòng “Đã làm gì” vào cuối card.

## Test phải viết
- Migration tạo đủ bảng/constraint trên DB rỗng.
- Transcript thiếu mã lớp/khoa/kỳ/hash/path bị từ chối.
- Một transcript lưu được nhiều signer và metadata TSA/validation.
- Audit event không thể update/delete qua repository thông thường.

## Định nghĩa "xong"
Các card backend dùng được model/migration ổn định mà không tự tạo bảng dùng chung.

## Cạm bẫy đã biết
Không để status và validation_status thành chuỗi tự do; không xóa chữ ký khi transcript bị rejected.

## Đã làm gì
(agent điền khi xong)

