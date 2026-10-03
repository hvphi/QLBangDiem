---
id: T-04
title: Xây dựng đăng nhập và phân quyền
status: in_progress
model: codex
effort: high
depends_on: ["T-03"]
touches:
  - "backend/core/src/main/java/app/webbangdiem/identity/**"
  - "backend/core/src/test/java/app/webbangdiem/identity/**"
  - "backend/contracts/auth/**"
prd_refs: ["§8.1", "§8.2"]
owner: null
started_at: 2026-09-28
finished_at: null
---

# T-04 · Xây dựng đăng nhập và phân quyền

## Mục tiêu
Cung cấp xác thực portal và authorization backend theo vai trò cùng phạm vi khoa/lớp để API nghiệp vụ dùng policy thống nhất.

## Ngữ cảnh cần biết
PRD đề cập JWT/OAuth2 và nhóm giảng viên, trưởng khoa, Khảo thí. T-01 quyết định issuer/provisioning; nếu chưa có IdP thật thì dùng adapter cấu hình được và issuer giả cho test.

## Phạm vi
**Trong:** JWT/OIDC verification, principal/role/scope mapping, policy service-to-service cho verification, contract auth.

**Ngoài:** UI đăng nhập (T-14), IdP production, catalog (T-05), phân quyền chỉ ở frontend.

## Đầu vào đã có
- Identity/scope schema từ T-03.
- Issuer/claims từ T-01.
- Backend skeleton từ T-02.

## Việc phải làm
1. Xác minh JWT/OIDC issuer cấu hình, signature, audience và thời hạn.
2. Chuẩn hóa principal, role, scope; mặc định từ chối nếu thiếu claim/quyền.
3. Cung cấp policy: giảng viên chỉ lớp phụ trách, trưởng khoa trong khoa được giao; Khảo thí theo quyền T-01.
4. Bảo vệ endpoint nội bộ bằng service identity/mTLS theo boundary.
5. Tạo contract auth riêng và mã lỗi ổn định.

## Quy ước bắt buộc
- Chỉ sửa đường dẫn trong touches của card đang làm. Không sửa card khác để đổi phụ thuộc/trạng thái.
- Không hard-code bí mật, URL CA/OCSP/CRL/TSA, issuer OIDC, thông tin MinIO/NAS hoặc Google Drive; dùng cấu hình môi trường được tài liệu hóa.
- Backend là điểm thực thi quyền cuối; UI không thay authorization.
- Không log bearer token/cookie hoặc dữ liệu định danh không cần thiết.
- Chỉ sửa package/contract/test ghi trong touches.

## Checklist đầu ra
- [ ] Backend compile theo CONVENTIONS.md.
- [ ] API tests xanh cho allow/deny và token sai/hết hạn.
- [ ] Không có quyền vượt scope.
- [ ] Không đụng file ngoài touches.
- [ ] Cập nhật status: review và finished_at trong frontmatter card này.
- [ ] Ghi 3–5 dòng “Đã làm gì” vào cuối card.

## Test phải viết
- Token đúng issuer/audience/chữ ký ánh xạ principal.
- Token hết hạn, sai issuer/audience/chữ ký bị từ chối.
- Giảng viên không truy cập transcript ngoài lớp phụ trách.
- Trưởng khoa không duyệt transcript ngoài khoa.
- Role/scope thiếu bị từ chối mặc định.

## Định nghĩa "xong"
API nghiệp vụ gọi chung policy để xác minh actor/scope mà không tự giải mã token.

## Cạm bẫy đã biết
Không tin role/scope do client gửi trong body/query.

## Đã làm gì
(agent điền khi xong)

