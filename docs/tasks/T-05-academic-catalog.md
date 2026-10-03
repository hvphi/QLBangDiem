---
id: T-05
title: Xây dựng danh mục học vụ và hạn nộp
status: in_progress
model: codex
effort: medium
depends_on: ["T-03", "T-04"]
touches:
  - "backend/core/src/main/java/app/webbangdiem/catalog/**"
  - "backend/core/src/test/java/app/webbangdiem/catalog/**"
  - "backend/contracts/catalog/**"
prd_refs: ["§8.2", "§8.3", "§8.4"]
owner: null
started_at: 2026-09-28
finished_at: null
---

# T-05 · Xây dựng danh mục học vụ và hạn nộp

## Mục tiêu
Cung cấp API tra cứu/quản lý năm học, học kỳ, khoa, lớp học phần và hạn nộp làm dữ liệu chuẩn cho nộp, duyệt và định tuyến.

## Ngữ cảnh cần biết
PRD quy định cây Năm học → Học kỳ → Khoa → Mã lớp học phần và metadata transcript gồm môn, giảng viên, khoa, năm học, học kỳ.

## Phạm vi
**Trong:** API catalog, validation, deadline config và quyền quản trị theo T-01.

**Ngoài:** Admin UI (T-17), object storage (T-07), tự suy ra deadline chưa cấu hình.

## Đầu vào đã có
- Domain/schema T-03.
- Auth/scope T-04.
- Kỳ/năm học/deadline rules từ docs/architecture/DECISIONS.md.

## Việc phải làm
1. Tạo CRUD/lookup API cho catalog với phân trang/tìm kiếm theo mã lớp/khoa/kỳ.
2. Kiểm tra quan hệ lớp-môn-giảng viên-khoa-kỳ.
3. Cung cấp deadline/status mở-đóng theo timezone đã chốt.
4. Ngăn sửa catalog tham chiếu làm đổi ý nghĩa transcript đã lưu; dùng lock/versioning theo T-01.
5. Ghi API contract riêng.

## Quy ước bắt buộc
- Chỉ sửa đường dẫn trong touches của card đang làm. Không sửa card khác để đổi phụ thuộc/trạng thái.
- Không hard-code bí mật, URL CA/OCSP/CRL/TSA, issuer OIDC, thông tin MinIO/NAS hoặc Google Drive; dùng cấu hình môi trường được tài liệu hóa.
- Chỉ sửa package/test/contract trong touches.
- Backend kiểm tra quyền quản trị và scope.
- Không tạo migration; schema thuộc T-03.

## Checklist đầu ra
- [ ] Backend compile theo CONVENTIONS.md.
- [ ] API tests tra cứu, validation, deadline và quyền xanh.
- [ ] Không rò dữ liệu ngoài scope.
- [ ] Không đụng file ngoài touches.
- [ ] Cập nhật status: review và finished_at trong frontmatter card này.
- [ ] Ghi 3–5 dòng “Đã làm gì” vào cuối card.

## Test phải viết
- Lớp có quan hệ hợp lệ được tạo.
- Mã lớp trùng trong cùng kỳ bị từ chối.
- Lookup theo kỳ/khoa trả đúng và không lộ khoa ngoài scope.
- Deadline đổi trạng thái đúng ngay trước/sau hạn theo timezone.

## Định nghĩa "xong"
Workflow có thể lấy metadata chuẩn và kiểm tra deadline qua API catalog.

## Cạm bẫy đã biết
Không ghép học kỳ từ chuỗi tùy ý; ẩn nút nộp sau hạn không phải biện pháp khóa.

## Đã làm gì
(agent điền khi xong)

