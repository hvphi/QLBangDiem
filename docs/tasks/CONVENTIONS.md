# Quy ước chung cho task cards

Baseline để tránh xung đột. T-01 hoàn thiện quyết định kiến trúc và lệnh cụ thể trước khi T-02 bắt đầu; các card sau tuân thủ bản đã chốt.

## Ranh giới file và sở hữu

- Chỉ sửa đường dẫn trong touches của card đang làm. Không sửa card khác để đổi phụ thuộc/trạng thái.
- T-02 sở hữu mọi manifest/package/build/CI. T-03 sở hữu migration và model miền dùng chung. T-11 sở hữu contract audit. T-14 sở hữu layout, auth UI và component dùng chung. Mỗi feature sở hữu source, API contract, tests và route riêng như card ghi.
- Các card backend hiện dành riêng namespace Java app.webbangdiem với mỗi module con độc lập; T-01 xác nhận namespace này và sửa touches đồng bộ trước khi T-02 bắt đầu nếu cần.
- Không sửa migration, API contract dùng chung, route shell hoặc manifest/build file thuộc card khác. Yêu cầu chủ file cập nhật trước và thêm phụ thuộc tương ứng.
- Không hard-code bí mật, URL CA/OCSP/CRL/TSA, issuer OIDC, thông tin MinIO/NAS hoặc Google Drive; dùng cấu hình môi trường được tài liệu hóa.

## Bất biến nghiệp vụ từ PRD

- Luồng: giảng viên ký → kiểm tra tự động → trưởng khoa ký/duyệt → lưu trữ. Chỉ chuyển trạng thái khi bước kiểm tra liên quan thành công.
- Ghi SHA-256 và metadata chữ ký. Không đổi byte PDF sau chữ ký giảng viên; chữ ký trưởng khoa dùng incremental save để giữ chữ ký trước.
- Không coi PDF có signature invalid, revoked/expired theo policy hoặc hash sai là hợp lệ. Lưu lý do verify phục vụ audit.
- Đường dẫn theo năm học/học kỳ/khoa/mã lớp; tên file theo quy chuẩn PRD. Chặn path traversal và tên file nguy hiểm.
- Upload, verify, duyệt/ký, khóa sửa và download phát sinh audit event append-only. Khóa ghi theo hạn nộp; không ghi đè bản đã ký.
- Backend kiểm tra quyền theo role và phạm vi; UI không thay thế authorization.

## Kiểm thử và hoàn tất

- Mỗi card viết test cho hành vi tại mục Test phải viết, không chỉ test implementation detail.
- T-01 chốt lệnh backend, frontend, API và E2E tại tài liệu quyết định. Dùng đúng lệnh theo stack; card UI bắt buộc có E2E.
- Lệnh ban đầu: backend compile/test dùng mvn -f backend/pom.xml verify/test; frontend typecheck/test dùng npm --prefix frontend run typecheck/test; E2E dùng npm --prefix frontend run e2e. T-02 phải cung cấp đúng scripts.
- Chỉ chuyển sang review khi việc và kiểm tra bắt buộc hoàn tất; cập nhật finished_at và ghi 3–5 dòng Đã làm gì.
- Nếu PRD mơ hồ về danh tính, vai trò, hạn nộp, từ chối/nộp lại, CA/TSA hay retention, ghi giả định/câu hỏi trong quyết định; không đặt policy nghiệp vụ ngầm.
