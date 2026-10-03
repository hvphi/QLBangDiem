---
id: T-18
title: Xây dựng sao lưu và khôi phục
status: in_progress
model: codex
effort: high
depends_on: ["T-07"]
touches:
  - "infra/backup/**"
  - "docs/ops/backup-recovery.md"
prd_refs: ["§8.2", "§8.5", "§8.6"]
owner: null
started_at: 2026-09-28
finished_at: null
---

# T-18 · Xây dựng sao lưu và khôi phục

## Mục tiêu
Sao lưu file/metadata theo lịch, mã hóa, phát hiện sync lỗi và cung cấp quy trình restore đã diễn tập.

## Ngữ cảnh cần biết
PRD yêu cầu secondary backup mã hóa mỗi ngày lúc 00:00 tới Google Drive Enterprise/ổ dự phòng; MinIO/NAS là primary. Lịch/timezone/retention theo T-01.

## Phạm vi
**Trong:** Job/scheduler, rclone/Drive adapter theo decision, encryption, checksum, retry/alert và restore runbook.

**Ngoài:** Credential thật, account Drive trường, sửa storage core T-07, retention pháp lý chưa chốt.

## Đầu vào đã có
- Storage path/object contract T-07.
- Schedule/encryption/retention T-01.
- Config pattern T-02.

## Việc phải làm
1. Tạo backup job idempotent, chỉ đọc primary và mã hóa trước khi rời môi trường.
2. Đồng bộ objects/metadata cùng manifest kiểm tra tính đầy đủ.
3. Retry có giới hạn, log trạng thái không lộ secret, alert thất bại.
4. Tạo restore runbook/script vào vùng tách biệt, không overwrite production mặc định.
5. Diễn tập restore bằng dữ liệu giả và ghi kết quả.

## Quy ước bắt buộc
- Chỉ sửa đường dẫn trong touches của card đang làm. Không sửa card khác để đổi phụ thuộc/trạng thái.
- Không hard-code bí mật, URL CA/OCSP/CRL/TSA, issuer OIDC, thông tin MinIO/NAS hoặc Google Drive; dùng cấu hình môi trường được tài liệu hóa.
- Không commit credential/key thật; dùng secret manager/config.
- Backup phải mã hóa và checksum verify.
- Chỉ sở hữu infra/backup và runbook trong touches.

## Checklist đầu ra
- [ ] Config/script lint và tests theo CONVENTIONS.md xanh.
- [ ] Restore drill hoàn tất, checksum khớp.
- [ ] Không xóa bản cũ trước khi bản mới verify.
- [ ] Không đụng file ngoài touches.
- [ ] Cập nhật status: review và finished_at trong frontmatter card này.
- [ ] Ghi 3–5 dòng “Đã làm gì” vào cuối card.

## Test phải viết
- Backup gồm đúng objects/manifest và output được mã hóa.
- Retry sau lỗi mạng không mất/nhân đôi dữ liệu ngoài policy.
- Checksum sai bị báo lỗi, không coi backup hoàn tất.
- Restore khôi phục fixture/hash/path vào vùng tách biệt.

## Định nghĩa "xong"
Backup định kỳ có thể kiểm tra và restore bằng cấu hình không chứa bí mật trong repo.

## Cạm bẫy đã biết
Không xóa bản cũ trước khi kiểm chứng bản mới; scheduler phải cấu hình timezone.

## Đã làm gì
(agent điền khi xong)

