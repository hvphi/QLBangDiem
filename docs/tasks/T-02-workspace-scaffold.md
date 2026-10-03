---
id: T-02
title: Dựng workspace và CI
status: in_progress
model: codex
effort: medium
depends_on: ["T-01"]
touches:
  - "backend/pom.xml"
  - "backend/core/pom.xml"
  - "backend/core/src/main/java/app/webbangdiem/CoreApplication.java"
  - "backend/verification/pom.xml"
  - "backend/verification/src/main/java/app/webbangdiem/verification/VerificationApplication.java"
  - "backend/verification/src/main/java/app/webbangdiem/verification/HealthController.java"
  - "backend/core/src/main/resources/application.yml"
  - "backend/core/src/main/resources/application-local.yml"
  - "frontend/package.json"
  - "frontend/tsconfig.json"
  - "frontend/next.config.ts"
  - "frontend/app/page.tsx"
  - "signing-bridge/package.json"
  - "infra/dev/compose.yaml"
  - ".github/workflows/ci.yml"
  - ".gitignore"
prd_refs: ["§8.1", "§8.5"]
owner: null
started_at: 2026-09-28
finished_at: null
---

# T-02 · Dựng workspace và CI

## Mục tiêu
Tạo khung build chạy cục bộ cho backend core, verification, frontend, signing bridge và dịch vụ phụ trợ; cung cấp CI nền để feature không cùng sửa manifest gốc.

## Ngữ cảnh cần biết
T-01 đã chốt stack, module boundaries và lệnh kiểm tra. Core và verification phải deploy được riêng; verification nằm sau biên bảo mật riêng.

## Phạm vi
**Trong:** Root/module manifests, skeleton khởi chạy rỗng, dev local, CI build/lint/typecheck/test theo convention.

**Ngoài:** Feature nghiệp vụ, migration, secret thật, cấu hình production hay policy gateway.

## Đầu vào đã có
- Quyết định và lệnh từ docs/tasks/CONVENTIONS.md, docs/architecture/DECISIONS.md.
- Repo hiện có tài liệu trong docs nhưng chưa có source application.

## Việc phải làm
1. Tạo project skeleton và mọi build manifest theo T-01; T-06/T-08 chỉ sở hữu source, contract và tài liệu trong touches.
2. Tạo cấu hình local cho PostgreSQL, Redis, MinIO bằng dữ liệu giả và config override.
3. Thêm health endpoint tối thiểu, không thêm business API.
4. Thêm CI gọi đúng lệnh trong CONVENTIONS.md, không chứa secret.
5. Đảm bảo feature có thể thêm code trong package riêng mà không sửa manifest gốc.

## Quy ước bắt buộc
- Chỉ sửa đường dẫn trong touches của card đang làm. Không sửa card khác để đổi phụ thuộc/trạng thái.
- Không hard-code bí mật, URL CA/OCSP/CRL/TSA, issuer OIDC, thông tin MinIO/NAS hoặc Google Drive; dùng cấu hình môi trường được tài liệu hóa.
- T-02 là chủ duy nhất của manifest gốc/CI.
- Không để dependency nghiệp vụ feature vào root nếu module có manifest riêng.
- Không lưu dữ liệu/secret môi trường vào Git.

## Checklist đầu ra
- [ ] Backend compile và frontend typecheck theo CONVENTIONS.md.
- [ ] CI chạy build/lint/typecheck/test nền trên pull request.
- [ ] Dev services khởi động bằng cấu hình mẫu không có secret.
- [ ] Không đụng file ngoài touches.
- [ ] Cập nhật status: review và finished_at trong frontmatter card này.
- [ ] Ghi 3–5 dòng “Đã làm gì” vào cuối card.

## Test phải viết
- Service khởi động và health endpoint trả healthy.
- Frontend shell khởi động và route không tồn tại trả 404 chuẩn.
- PostgreSQL/Redis/MinIO dùng được sau lần khởi động sạch.

## Định nghĩa "xong"
CI xanh và ứng dụng cùng dependency local khởi động theo tài liệu mà không cần secret thật.

## Cạm bẫy đã biết
Không tạo API/schema tạm mà feature sau phải sửa; CI không phụ thuộc dịch vụ production.

## Đã làm gì
(agent điền khi xong)

