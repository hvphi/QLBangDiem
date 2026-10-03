---
id: T-15
title: Xây dựng portal giảng viên
status: in_progress
model: codex
effort: high
depends_on: ["T-08", "T-09", "T-14"]
touches:
  - "frontend/app/lecturer/**"
  - "frontend/src/features/lecturer/**"
prd_refs: ["§8.1", "§8.3"]
owner: null
started_at: 2026-09-28
finished_at: null
---

# T-15 · Xây dựng portal giảng viên

## Mục tiêu
Cho giảng viên xem lớp được giao, chọn PDF đã ký, nộp bảng điểm và theo dõi trạng thái/lý do cần xử lý.

## Ngữ cảnh cần biết
PRD: giảng viên xuất PDF, ký USB Token hoặc vSignPDF, upload, rồi chờ trưởng khoa. Chỉ server verify quyết định trạng thái hợp lệ.

## Phạm vi
**Trong:** Route giảng viên, lớp/deadline, upload progress, signing bridge và trạng thái submission.

**Ngoài:** Tạo bảng điểm từ SIS, xác nhận signature phía client, approve cấp khoa, shared shell.

## Đầu vào đã có
- Upload/status API T-09.
- Signing bridge T-08.
- Shell/auth T-14 và catalog T-05.
- Design conventions T-01.

## Việc phải làm
1. Hiển thị đúng lớp học phần và deadline của actor.
2. Cho phép ký bằng bridge hoặc chọn PDF đã ký bởi vSignPDF.
3. Hiển thị file/lớp/signer metadata, progress và kết quả server.
4. Hiển thị lý do invalid/rejected và retry/resubmit chỉ khi policy cho phép.
5. Không làm bản mới trông như hồ sơ cũ chưa từng tồn tại.

## Quy ước bắt buộc
- Chỉ sửa đường dẫn trong touches của card đang làm. Không sửa card khác để đổi phụ thuộc/trạng thái.
- Không hard-code bí mật, URL CA/OCSP/CRL/TSA, issuer OIDC, thông tin MinIO/NAS hoặc Google Drive; dùng cấu hình môi trường được tài liệu hóa.
- Chỉ sửa route/feature giảng viên.
- Dùng shared shell T-14; không sửa global layout/styles.
- Client hiển thị verification từ server, không tự approve.

## Checklist đầu ra
- [ ] Frontend typecheck/unit tests theo CONVENTIONS.md xanh.
- [ ] E2E upload success/failure xanh.
- [ ] Không đụng file ngoài touches.
- [ ] Không đụng file ngoài touches.
- [ ] Cập nhật status: review và finished_at trong frontmatter card này.
- [ ] Ghi 3–5 dòng “Đã làm gì” vào cuối card.

## Test phải viết
- Giảng viên thấy đúng lớp/deadline.
- File hợp lệ hiển thị chờ duyệt.
- File invalid hiện lý do và không hiện như nộp thành công.
- Hủy/mất mạng cho phép retry an toàn không duplicate.
- Role khác không vào route giảng viên.

## Định nghĩa "xong"
Giảng viên hoàn thành được luồng ký/nộp và thấy trạng thái đáng tin cậy.

## Cạm bẫy đã biết
Không báo hợp lệ dựa trên bridge; status phải lấy từ server sau verify.

## Đã làm gì
(agent điền khi xong)

