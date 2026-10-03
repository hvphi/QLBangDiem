---
id: T-08
title: Tích hợp cầu nối ký số USB Token
status: in_progress
model: codex
effort: high
depends_on: ["T-01", "T-02"]
touches:
  - "signing-bridge/src/**"
  - "docs/signing-bridge/**"
prd_refs: ["§8.2", "§8.3", "§8.5"]
owner: null
started_at: 2026-09-28
finished_at: null
---

# T-08 · Tích hợp cầu nối ký số USB Token

## Mục tiêu
Cho phép ký PDF cục bộ bằng USB Token/PKCS#11 và giữ đường ký ngoài bằng vSignPDF.

## Ngữ cảnh cần biết
PRD yêu cầu WebPKI native bridge/browser extension với token VGCA. Hệ điều hành, token, driver và cách phân phối phải do T-01 chốt hoặc ghi ma trận hỗ trợ.

## Phạm vi
**Trong:** Adapter browser/agent, giao thức ký, consent, trả PDF đã ký, hướng dẫn cài đặt/chẩn đoán.

**Ngoài:** Lưu private key, ký thay người dùng trên server, verify chữ ký (T-06), phát hành chứng thư.

## Đầu vào đã có
- Contract/platform mục tiêu T-01.
- Workspace signing-bridge T-02.
- Quy tắc verify lại ở backend T-06.

## Việc phải làm
1. Tạo adapter PKCS#11 qua agent/bridge đã chọn; nhận diện token/certificate trước khi ký.
2. Yêu cầu user xác nhận từng lần; không giữ PIN/private key lâu hơn phiên thao tác.
3. Trả file đã ký cho caller, không upload trước consent.
4. Trả lỗi rõ cho thiếu token, PIN sai, certificate hết hạn hoặc agent không tương thích.
5. Tài liệu hóa fallback ký bằng vSignPDF rồi upload.

## Quy ước bắt buộc
- Chỉ sửa đường dẫn trong touches của card đang làm. Không sửa card khác để đổi phụ thuộc/trạng thái.
- Không hard-code bí mật, URL CA/OCSP/CRL/TSA, issuer OIDC, thông tin MinIO/NAS hoặc Google Drive; dùng cấu hình môi trường được tài liệu hóa.
- Không gửi PIN/private key lên backend, log hoặc telemetry.
- Bridge không tuyên bố file hợp lệ; T-06 xác minh lại.
- Chỉ sửa signing-bridge và docs trong touches.

## Checklist đầu ra
- [ ] Build/typecheck/test theo CONVENTIONS.md xanh.
- [ ] Bridge/E2E tests cho ký, hủy và lỗi xanh.
- [ ] Không có credential trong code/log.
- [ ] Không đụng file ngoài touches.
- [ ] Cập nhật status: review và finished_at trong frontmatter card này.
- [ ] Ghi 3–5 dòng “Đã làm gì” vào cuối card.

## Test phải viết
- Agent phát hiện token và trả certificate metadata không nhạy cảm.
- Hủy hoặc PIN sai không gửi file ký và không log PIN.
- Ký thành công trả PDF backend có thể verify.
- Agent mất kết nối hiển thị fallback vSignPDF.

## Định nghĩa "xong"
Người dùng ký trên máy mình hoặc dùng vSignPDF, và backend luôn xác thực lại.

## Cạm bẫy đã biết
PKCS#11 phụ thuộc OS/driver/token; mock không thay được ma trận tương thích.

## Đã làm gì
(agent điền khi xong)

