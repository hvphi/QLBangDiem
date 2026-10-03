---
id: T-07
title: Xây dựng lưu trữ và khóa sửa
status: in_progress
model: codex
effort: high
depends_on: ["T-03", "T-05", "T-11"]
touches:
  - "backend/core/src/main/java/app/webbangdiem/storage/**"
  - "backend/core/src/test/java/app/webbangdiem/storage/**"
  - "backend/contracts/storage/**"
prd_refs: ["§8.2", "§8.3", "§8.6"]
owner: null
started_at: 2026-09-28
finished_at: null
---

# T-07 · Xây dựng lưu trữ và khóa sửa

## Mục tiêu
Lưu PDF bất biến theo cây thư mục học vụ, ghi SHA-256/vị trí và khóa chỉnh sửa khi hết hạn nộp.

## Ngữ cảnh cần biết
PRD quy định Năm học/Học kỳ/Khoa/Mã lớp, tên MaLHP_TenMon_TenGiangVien.pdf, MinIO/NAS và WORM/readonly sau hạn.

## Phạm vi
**Trong:** Storage port/adapter, path builder an toàn, checksum, read/download authz và write lock theo deadline.

**Ngoài:** Upload workflow (T-09), backup cloud (T-18), production secrets/config (T-19).

## Đầu vào đã có
- Schema T-03, metadata/deadline API T-05.
- Path/name/timezone convention trong docs/tasks/CONVENTIONS.md.

## Việc phải làm
1. Tạo storage port/adapter theo T-01, hỗ trợ object immutable/version phù hợp.
2. Tạo path deterministic từ năm học/kỳ/khoa/lớp và sanitize segment.
3. Tính/xác minh SHA-256 khi ghi/đọc; lưu object ID/path qua contract.
4. Không ghi đè bản ký; mỗi revision có immutable key.
5. Từ chối write sau deadline và bật WORM/readonly; phát audit event lock/read/download qua contract T-11.
6. Ghi rõ giới hạn nếu NAS không hỗ trợ WORM thật.

## Quy ước bắt buộc
- Chỉ sửa đường dẫn trong touches của card đang làm. Không sửa card khác để đổi phụ thuộc/trạng thái.
- Không hard-code bí mật, URL CA/OCSP/CRL/TSA, issuer OIDC, thông tin MinIO/NAS hoặc Google Drive; dùng cấu hình môi trường được tài liệu hóa.
- Không ghép path từ chuỗi client; metadata/quyền lấy từ catalog.
- Không sửa PDF đã ký, không lưu secret trong source.
- Không tạo migration/sửa feature khác.

## Checklist đầu ra
- [ ] Backend compile theo CONVENTIONS.md.
- [ ] Storage tests path/hash/readonly/backend failure xanh.
- [ ] Policy write-lock áp dụng ở storage boundary.
- [ ] Không đụng file ngoài touches.
- [ ] Cập nhật status: review và finished_at trong frontmatter card này.
- [ ] Ghi 3–5 dòng “Đã làm gì” vào cuối card.

## Test phải viết
- Metadata hợp lệ định tuyến đúng năm/kỳ/khoa/lớp.
- Path traversal bị sanitize hoặc từ chối.
- Hash sau lưu khớp; đổi byte bị phát hiện.
- Write sau deadline bị từ chối, read theo quyền vẫn được.
- Revision mới không ghi đè bản ký trước.
- Lock và download phát audit event đúng transcript/actor qua contract T-11.

## Định nghĩa "xong"
Workflow lưu/đọc file theo PRD, phát hiện biến đổi và khóa ghi sau hạn.

## Cạm bẫy đã biết
Readonly ở DB không đủ nếu object store còn cho overwrite.

## Đã làm gì
(agent điền khi xong)

