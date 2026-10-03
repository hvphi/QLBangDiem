---
id: T-06
title: Xây dựng engine kiểm tra chữ ký số
status: in_progress
model: codex
effort: high
depends_on: ["T-01", "T-02"]
touches:
  - "backend/verification/src/main/java/app/webbangdiem/verification/api/**"
  - "backend/verification/src/main/java/app/webbangdiem/verification/validation/**"
  - "backend/verification/src/test/java/app/webbangdiem/verification/validation/**"
  - "backend/contracts/verification/**"
prd_refs: ["§8.1", "§8.2", "§8.3", "§8.6"]
owner: null
started_at: 2026-09-28
finished_at: null
---

# T-06 · Xây dựng engine kiểm tra chữ ký số

## Mục tiêu
Kiểm tra chữ ký PDF, tính toàn vẹn, chứng thư và timestamp; trả kết quả có cấu trúc cho core trước khi file được coi là hợp lệ.

## Ngữ cảnh cần biết
PRD yêu cầu PAdES, hash integrity, CA VGCA/BCA, OCSP/CRL, TSA RFC 3161, hai signer và engine cô lập DMZ. Endpoint thật chỉ lấy từ cấu hình.

## Phạm vi
**Trong:** Service cô lập, request/response contract, PDF signature parsing, chain/revocation/TSA verification theo policy T-01.

**Ngoài:** Ký bằng USB Token (T-08), transcript workflow (T-09/T-10), dùng test certificate làm production.

## Đầu vào đã có
- Boundary/policy T-01.
- Backend skeleton/contracts T-02.
- PDF fixture tổng hợp, không có dữ liệu điểm thật.

## Việc phải làm
1. Tạo API nội bộ có service identity và giới hạn kích thước/MIME/timeout.
2. Đọc chữ ký CMS/PAdES-LTV, signer, chain, signed byte ranges, timestamp token và dữ liệu revocation đã nhúng.
3. Phát hiện sửa PDF, scan lại, signature invalid/revoked/expired và thiếu chữ ký.
4. Kiểm tra OCSP/CRL; cung cấp TSA client RFC 3161 qua adapter cấu hình và phân biệt unavailable/unknown.
5. Trả kết quả, SHA-256 và lý do; không log/lưu toàn bộ PDF ngoài policy.

## Quy ước bắt buộc
- Chỉ sửa đường dẫn trong touches của card đang làm. Không sửa card khác để đổi phụ thuộc/trạng thái.
- Không hard-code bí mật, URL CA/OCSP/CRL/TSA, issuer OIDC, thông tin MinIO/NAS hoặc Google Drive; dùng cấu hình môi trường được tài liệu hóa.
- Fail closed; lỗi CA/TSA không được coi là valid.
- Không đưa private key/certificate secret/PDF thật vào log/test.
- Không truy cập DB/NAS ngoài boundary T-01.

## Checklist đầu ra
- [ ] Backend compile/test theo CONVENTIONS.md.
- [ ] API verification tests cho valid/invalid/revoked/expired/tampered xanh.
- [ ] Verification chỉ truy cập qua contract nội bộ.
- [ ] Không đụng file ngoài touches.
- [ ] Cập nhật status: review và finished_at trong frontmatter card này.
- [ ] Ghi 3–5 dòng “Đã làm gì” vào cuối card.

## Test phải viết
- Chữ ký hợp lệ trả VALID cùng signer/hash.
- Byte sửa sau ký làm integrity check thất bại.
- Revoked/expired/chain không tin cậy không trả VALID.
- Thiếu chữ ký trả status/reason theo policy.
- OCSP/TSA timeout trả UNKNOWN/UNAVAILABLE.

## Định nghĩa "xong"
Core phân biệt hợp lệ, không hợp lệ và không thể xác minh qua service được bảo vệ.

## Cạm bẫy đã biết
Không chỉ đọc tên signer từ metadata; phải kiểm tra byte range, cryptographic signature và chain.

## Đã làm gì
(agent điền khi xong)

