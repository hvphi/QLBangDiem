# Quyết định kiến trúc — QLBangDiem

Trạng thái: đề xuất triển khai theo PRD; cần thay các giá trị môi trường thực của trường trước production.

## Quyết định đã chốt cho implementation

| Chủ đề | Quyết định | Lý do / ranh giới |
|---|---|---|
| Backend | Java 21, Spring Boot 3.5.16, Maven 3.9.16 | PRD ưu tiên hệ sinh thái PDF/crypto; Java là lựa chọn cụ thể trong các phương án PRD nêu. |
| Frontend | Next.js 16.3.6, React, TypeScript, npm | Phù hợp portal nhiều vai trò; dùng app router và E2E. |
| PDF/crypto libraries | PDFBox 3.0.8 và Bouncy Castle 1.86 | Dùng parser PDF/CMS; engine phải fail closed khi chưa được cấp trust anchors/CA/TSA production. |
| Core | Modular monolith | Grade/workflow, catalog, storage, audit, notification, identity là các module/package riêng trong backend/core. |
| Verification | Service Spring Boot deployable riêng, đặt trong DMZ | Không truy cập DB/NAS; chỉ core gọi qua API nội bộ xác thực service-to-service. |
| Database/cache/object | PostgreSQL 16, Redis, MinIO | Dùng cho dữ liệu giao dịch, session/cache và PDF; local dev có thể dùng profile riêng nhưng production phải dùng các dịch vụ này. |
| Gateway | Nginx | Route portal/core; verification không có public route. TLS/certificate được cấu hình ở môi trường triển khai. |
| Java namespace | app.webbangdiem | Dành trước package path trong task cards; đổi trước khi T-02 scaffold nếu tổ chức có namespace bắt buộc. |
| Workflow | LECTURER_SIGNED → DEPT_APPROVED → ARCHIVED; REJECTED là nhánh kết thúc/đợi xử lý theo policy | Không tự chuyển trạng thái khi verify chưa hợp lệ; quy tắc re-submit phải được trường xác nhận trước production. |
| PDF | Không sửa byte sau chữ ký giảng viên; trưởng khoa ký bằng incremental update | Bảo toàn chữ ký đầu và đáp ứng yêu cầu ký nhiều cấp trong PRD. |
| PKI | Browser/local signer dùng PKCS#11; vSignPDF upload là fallback | Server không nhận private key/PIN. Mọi file signed được verify lại server-side. |
| CA/TSA | Adapter cấu hình được cho VGCA/BCA OCSP/CRL và RFC 3161 TSA | Không đưa URL hoặc trust anchors production vào source. Thiếu cấu hình trust thì trạng thái không được nâng thành VALID. |
| Storage | Object immutable theo năm học/học kỳ/khoa/mã lớp; SHA-256 | WORM thực sự tùy khả năng MinIO/NAS được trường cấp; không giả lập WORM chỉ bằng UI. |
| Auth | OIDC/JWT issuer cấu hình theo môi trường | Không có issuer/claims production trong repo. Local dev identity chỉ để phát triển và không được bật khi production. |
| Backup | Job mã hóa hằng ngày; rclone/Drive adapter cấu hình ngoài source | Credentials, encryption key, timezone và retention do vận hành cung cấp. |

## Các câu hỏi cần trường cung cấp trước production

1. OIDC issuer/client/claims, cách cấp account và danh sách role/scope chính thức.
2. CA trust anchors, OCSP/CRL URLs, TSA endpoint/credentials và PDF fixture ký từ test environment.
3. OS, model USB Token, PKCS#11 library/driver và cách phân phối/allow-list local signing agent.
4. MinIO/NAS endpoints, chính sách WORM/object lock, retention và backup target/key/timezone.
5. Deadline theo từng năm học/học kỳ/khoa, timezone vận hành, quy tắc từ chối/nộp lại và ai quản lý catalog.
6. Host/namespace/TLS certificate/network zone, secret manager và quy trình rollback cho deployment.

## Lệnh build và kiểm tra dự kiến

- Backend compile: mvn -f backend/pom.xml verify.
- Backend tests: mvn -f backend/pom.xml test.
- Frontend typecheck: npm --prefix frontend run typecheck.
- Frontend tests: npm --prefix frontend test.
- E2E: npm --prefix frontend run e2e.

Các lệnh trên phải được T-02 thực hiện thành scripts ổn định. Không đánh dấu task hoàn tất nếu dependency hạ tầng production chưa có; khi đó ghi rõ kiểm tra đã chạy trên môi trường local/test.
