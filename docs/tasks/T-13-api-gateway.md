---
id: T-13
title: Cấu hình API gateway và bảo vệ biên
status: in_progress
model: codex
effort: medium
depends_on: ["T-02", "T-04"]
touches:
  - "infra/gateway/**"
  - "docs/ops/gateway.md"
prd_refs: ["§8.1", "§8.6"]
owner: null
started_at: 2026-09-28
finished_at: null
---

# T-13 · Cấu hình API gateway và bảo vệ biên

## Mục tiêu
Đặt gateway nội bộ làm điểm vào portal/API, giới hạn lưu lượng và chuyển tiếp request an toàn tới core và verification.

## Ngữ cảnh cần biết
PRD gợi ý Nginx/Kong, TLS 1.3, rate limiting, WAF, JWT/OAuth2. T-01 chốt sản phẩm, topology và TLS policy.

## Phạm vi
**Trong:** Route, TLS mẫu, rate limits, headers, health checks, body/timeouts và hướng dẫn cấu hình.

**Ngoài:** Chứng thư production, cấu hình IdP, production deployment T-19, authorization nghiệp vụ T-04.

## Đầu vào đã có
- Topology/gateway choice T-01.
- API/health ports T-02/T-04.
- Verification boundary T-06.

## Việc phải làm
1. Tạo route frontend, core API và verification internal-only.
2. Thiết lập TLS policy, security headers, size/timeouts và rate limits theo endpoint.
3. Không để verification public; chỉ core/service identity truy cập.
4. Ẩn bearer token/PDF khỏi access log; thêm correlation ID.
5. Tài liệu hóa cập nhật certificate và chẩn đoán không lộ secret.

## Quy ước bắt buộc
- Chỉ sửa đường dẫn trong touches của card đang làm. Không sửa card khác để đổi phụ thuộc/trạng thái.
- Không hard-code bí mật, URL CA/OCSP/CRL/TSA, issuer OIDC, thông tin MinIO/NAS hoặc Google Drive; dùng cấu hình môi trường được tài liệu hóa.
- Không sửa infra/dev, infra/deploy hoặc package auth.
- Gateway không thay backend authorization; không tin forwarded identity từ client.
- Không commit private key/cert thật.

## Checklist đầu ra
- [ ] Config lint theo CONVENTIONS.md xanh.
- [ ] Route/rate-limit/body-limit/verification-private tests xanh.
- [ ] Không có secret trong config.
- [ ] Không đụng file ngoài touches.
- [ ] Cập nhật status: review và finished_at trong frontmatter card này.
- [ ] Ghi 3–5 dòng “Đã làm gì” vào cuối card.

## Test phải viết
- Request đúng host/path tới service đúng và có correlation ID.
- Burst vượt hạn mức trả 429; request thường vẫn qua.
- Body vượt giới hạn bị chặn trước backend.
- Không có public route tới verification.
- Log không có Authorization header hoặc PDF body.

## Định nghĩa "xong"
Gateway bảo vệ routes, verify vẫn private và policy được config-test kiểm tra.

## Cạm bẫy đã biết
Không bật CORS wildcard cùng credentials; chỉ tin forwarded headers từ proxy trong trust boundary.

## Đã làm gì
(agent điền khi xong)

