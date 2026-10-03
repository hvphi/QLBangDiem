---
id: T-20
title: Kiểm thử chấp nhận và gia cố hệ thống
status: in_progress
model: codex
effort: high
depends_on: ["T-09", "T-10", "T-12", "T-15", "T-16", "T-17", "T-19"]
touches:
  - "tests/integration/**"
  - "frontend/e2e/**"
  - "docs/acceptance/**"
prd_refs: ["§8.1", "§8.2", "§8.3", "§8.4", "§8.6"]
owner: null
started_at: 2026-09-28
finished_at: null
---

# T-20 · Kiểm thử chấp nhận và gia cố hệ thống

## Mục tiêu
Chứng minh luồng nghiệp vụ và bất biến bảo mật xuyên portal, API, verification, storage, audit và backup trước nghiệm thu.

## Ngữ cảnh cần biết
Đây là kiểm thử liên-module; unit/API tests thuộc card feature. Chỉ dùng PDF/certificate fixture hoặc môi trường test CA được phép.

## Phạm vi
**Trong:** Integration/E2E, role/scope, signature/tampering, path, audit, backup/restore và acceptance matrix.

**Ngoài:** Sửa feature code trực tiếp, pentest production, dữ liệu điểm thật, benchmark không có tiêu chí.

## Đầu vào đã có
- T-09/T-10 workflows; T-12 notifications; T-15/T-16/T-17 portals; T-19 deployment mẫu.
- Test fixtures/lệnh tại CONVENTIONS.md.

## Việc phải làm
1. Viết E2E giảng viên ký/nộp → verify → trưởng khoa nhận notification → ký/duyệt → archive.
2. Viết nhánh invalid/tampered/revoked, deadline đóng và reject/resubmit theo policy.
3. Kiểm tra role/scope, audit append-only, download log, backup checksum/restore.
4. Kiểm tra CA/TSA/storage/gateway unavailable không tạo status valid sai hoặc mất audit.
5. Ghi acceptance matrix, setup, kết quả, lỗi còn lại và điều kiện nghiệm thu.

## Quy ước bắt buộc
- Chỉ sửa đường dẫn trong touches của card đang làm. Không sửa card khác để đổi phụ thuộc/trạng thái.
- Không hard-code bí mật, URL CA/OCSP/CRL/TSA, issuer OIDC, thông tin MinIO/NAS hoặc Google Drive; dùng cấu hình môi trường được tài liệu hóa.
- Chỉ tạo/sửa integration tests, E2E tests và acceptance docs trong touches.
- Bug được chuyển cho card sở hữu; không vá feature tại đây.
- Không dùng endpoint/key/data production.

## Checklist đầu ra
- [ ] Typecheck và test bắt buộc theo CONVENTIONS.md xanh.
- [ ] E2E bắt buộc xanh trên deployment mẫu.
- [ ] Acceptance matrix ghi kết quả và ngoại lệ.
- [ ] Không đụng file ngoài touches.
- [ ] Cập nhật status: review và finished_at trong frontmatter card này.
- [ ] Ghi 3–5 dòng “Đã làm gì” vào cuối card.

## Test phải viết
- Luồng thành công từ chữ ký GV đến archive có hai chữ ký.
- Tampered/revoked/TSA unavailable không archive như hợp lệ.
- Sai role/scope bị chặn ở UI và API.
- Hết deadline khóa nộp/ghi nhưng audit đủ.
- Restore backup thành công và SHA-256 fixture khớp.

## Định nghĩa "xong"
Acceptance matrix bao phủ luồng chính và lỗi trọng yếu; test bắt buộc xanh hoặc ngoại lệ được chủ dự án chấp thuận và ghi rõ.

## Cạm bẫy đã biết
Mock verification quá mức che lỗi PDF thật; cần fixture ký hợp lệ từ môi trường test được kiểm soát.

## Đã làm gì
(agent điền khi xong)

