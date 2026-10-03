---
id: T-10
title: Xây dựng duyệt và ký cấp trưởng khoa
status: in_progress
model: codex
effort: high
depends_on: ["T-08", "T-09"]
touches:
  - "backend/core/src/main/java/app/webbangdiem/approval/**"
  - "backend/core/src/test/java/app/webbangdiem/approval/**"
  - "backend/contracts/approval/**"
prd_refs: ["§8.2", "§8.3", "§8.4", "§8.6"]
owner: null
started_at: 2026-09-28
finished_at: null
---

# T-10 · Xây dựng duyệt và ký cấp trưởng khoa

## Mục tiêu
Cho trưởng khoa xem hồ sơ chờ, ký/duyệt cấp 2 hoặc từ chối theo policy, xác thực đủ hai chữ ký rồi lưu bản hoàn tất.

## Ngữ cảnh cần biết
PRD yêu cầu chữ ký trưởng khoa append incremental để giữ chữ ký giảng viên; sau đó kiểm tra đủ hai chữ ký rồi routing lưu trữ.

## Phạm vi
**Trong:** Review/approve/reject API, scope checks, verify hai signature, chuyển trạng thái và phát audit/workflow events.

**Ngoài:** UI (T-16), USB bridge (T-08), sửa điểm hoặc tự đặt policy reject/resubmit.

## Đầu vào đã có
- Transcript LECTURER_SIGNED từ T-09.
- T-06 verify, T-07 storage, T-11 audit contract, T-08 signing client.
- Reject/resubmit decision từ T-01.

## Việc phải làm
1. Tạo review queue và status transitions theo khoa/scope.
2. Approve chỉ nhận PDF có chữ ký trưởng khoa và xác minh cả hai signer.
3. So sánh signed byte range/hash bảo đảm incremental save giữ chữ ký đầu.
4. Chỉ sau verify thành công mới lưu immutable revision và cập nhật DEPT_APPROVED/ARCHIVED theo policy.
5. Reject yêu cầu lý do, giữ file/audit và áp trạng thái theo T-01.

## Quy ước bắt buộc
- Chỉ sửa đường dẫn trong touches của card đang làm. Không sửa card khác để đổi phụ thuộc/trạng thái.
- Không hard-code bí mật, URL CA/OCSP/CRL/TSA, issuer OIDC, thông tin MinIO/NAS hoặc Google Drive; dùng cấu hình môi trường được tài liệu hóa.
- Kiểm tra role/department scope ở backend mỗi request.
- Không overwrite PDF giảng viên; tạo revision bất biến.
- Chỉ sửa package/test/contract trong touches.

## Checklist đầu ra
- [ ] Backend compile/test theo CONVENTIONS.md.
- [ ] Approve/reject/authorization API tests xanh.
- [ ] Invalid second signature không archive.
- [ ] Không đụng file ngoài touches.
- [ ] Cập nhật status: review và finished_at trong frontmatter card này.
- [ ] Ghi 3–5 dòng “Đã làm gì” vào cuối card.

## Test phải viết
- Trưởng khoa đúng scope ký incremental hợp lệ chuyển trạng thái đúng.
- PDF làm hỏng chữ ký đầu hoặc thiếu signer bị từ chối.
- Trưởng khoa ngoài khoa/giảng viên không thể approve.
- Reject cần lý do và không xóa PDF/audit.
- Retry không tạo chữ ký/revision trùng ngoài policy.

## Định nghĩa "xong"
Chỉ bản có hai chữ ký hợp lệ được lưu như bản đã duyệt.

## Cạm bẫy đã biết
Không tái serialize toàn bộ PDF khi thêm chữ ký thứ hai.

## Đã làm gì
(agent điền khi xong)

