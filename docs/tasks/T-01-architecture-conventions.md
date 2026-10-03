---
id: T-01
title: Chốt kiến trúc và quy ước triển khai
status: review
model: codex
effort: high
depends_on: []
touches:
  - "docs/architecture/DECISIONS.md"
  - "docs/tasks/CONVENTIONS.md"
prd_refs: ["§8.1", "§8.2", "§8.5", "§8.6"]
owner: null
started_at: 2026-09-28
finished_at: 2026-09-28
---

# T-01 · Chốt kiến trúc và quy ước triển khai

## Mục tiêu
Chuyển các lựa chọn còn mở trong PRD thành quyết định nhất quán để task sau làm độc lập; ghi rõ phần chưa thể chốt do thiếu thông tin nghiệp vụ/hạ tầng.

## Ngữ cảnh cần biết
PRD cho phép Modular Monolith hoặc microservices nhẹ, Java/Spring Boot hoặc Go, React hoặc Next.js, MinIO hoặc NAS, Nginx hoặc Kong. Đồng thời yêu cầu engine xác thực cô lập DMZ, OAuth2/JWT, VGCA/BCA, TSA và triển khai nội bộ.

## Phạm vi
**Trong:** Chốt stack, ranh giới module/service, giao thức core-verification, OIDC, event, cấu hình môi trường và lệnh build/test; hoàn thiện CONVENTIONS.md.

**Ngoài:** Viết mã ứng dụng, chọn nhà cung cấp CA/TSA/IdP thay trường, đặt policy pháp lý hoặc quy trình nộp lại chưa có trong PRD.

## Đầu vào đã có
- docs/PRD.md, mục 8.
- HuongDan ky so.docx ở thư mục gốc, nguồn quy trình ký số được PRD viện dẫn.
- docs/tasks/CONVENTIONS.md (baseline cần cập nhật).
- docs/tasks/README.md (bản đồ sở hữu file và phụ thuộc).

## Việc phải làm
1. Lập bảng quyết định cho lựa chọn công nghệ và lý do dựa trên PRD.
2. Chốt core modular monolith và verification engine cô lập; quy định auth service-to-service cùng dữ liệu được đi qua.
3. Chốt API/event contracts, versioning, migration, lỗi và idempotency ở mức đủ cho các card.
4. Đối chiếu hướng dẫn ký số có sẵn; chốt nơi tạo chữ ký PAdES-LTV, nhúng dữ liệu CRL/OCSP và gọi TSA RFC 3161; ghi luồng vSignPDF/USB Token, lưu trữ và điểm chưa rõ.
5. Ghi câu hỏi về IdP, vai trò, hạn/khóa, reject/resubmit, CA/TSA và backup; không chặn scaffold bằng giả định ngầm.
6. Xác nhận namespace Java app.webbangdiem (hoặc thay đồng bộ các touches nếu có quy ước trường khác), rồi cập nhật lệnh backend/frontend/API/E2E và quy ước tại docs/tasks/CONVENTIONS.md.

## Quy ước bắt buộc
- Chỉ sửa đường dẫn trong touches của card đang làm. Không sửa card khác để đổi phụ thuộc/trạng thái.
- Không hard-code bí mật, URL CA/OCSP/CRL/TSA, issuer OIDC, thông tin MinIO/NAS hoặc Google Drive; dùng cấu hình môi trường được tài liệu hóa.
- Giữ bất biến chữ ký và audit trong docs/tasks/CONVENTIONS.md.
- Phân biệt quyết định có căn cứ PRD với đề xuất cần xác nhận.
- Không đưa secret, chứng thư riêng, endpoint nội bộ hoặc dữ liệu thật vào tài liệu.

## Checklist đầu ra
- [x] Quyết định stack và ranh giới thành phần được ghi, có lý do và tác động tới task.
- [x] Câu hỏi mở/giả định không bị lẫn với yêu cầu đã chốt.
- [x] Lệnh build/typecheck/test được cập nhật trong CONVENTIONS.md.
- [ ] Không đụng file ngoài touches.
- [ ] Cập nhật status: review và finished_at trong frontmatter card này.
- [ ] Ghi 3–5 dòng “Đã làm gì” vào cuối card.

## Test phải viết
- Không áp dụng test mã; đối chiếu từng quyết định với mục PRD được tham chiếu.
- Kiểm tra phụ thuộc và đường dẫn sở hữu trong README không mâu thuẫn.

## Định nghĩa "xong"
Một người khác có thể dựng kiến trúc và chọn đúng lệnh kiểm tra chỉ từ quyết định và quy ước đã ghi.

## Cạm bẫy đã biết
PRD nêu tên công nghệ nhưng có chỗ là phương án thay thế. Không giả định endpoint thật của VGCA/TSA hay nhà cung cấp danh tính.

## Đã làm gì
Đã chốt Spring Boot modular monolith + verification service riêng và Next.js/TypeScript.
Đã ghi các giả định và giá trị hạ tầng bắt buộc cung cấp trước production.
Đã xác nhận namespace Java và lệnh build/test cho các task triển khai.

