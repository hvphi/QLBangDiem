---
id: T-11
title: Tạo audit trail append-only
status: in_progress
model: codex
effort: high
depends_on: ["T-03", "T-04"]
touches:
  - "backend/core/src/main/java/app/webbangdiem/audit/**"
  - "backend/core/src/test/java/app/webbangdiem/audit/**"
  - "backend/contracts/audit/**"
prd_refs: ["§8.2", "§8.4", "§8.6"]
owner: null
started_at: 2026-09-28
finished_at: null
---

# T-11 · Tạo audit trail append-only

## Mục tiêu
Ghi có thể truy vết các thao tác upload, verify, ký/duyệt, khóa sửa và download; công bố contract để module khác tích hợp không cần sửa audit.

## Ngữ cảnh cần biết
PRD yêu cầu audit trail non-repudiation và log bất biến phục vụ thanh tra. Tối thiểu phải nhận diện actor, action, object, time và outcome.

## Phạm vi
**Trong:** Versioned audit event, recorder/listener, append-only repository và query phân quyền.

**Ngoài:** Sửa workflow producers (T-07/T-09/T-10), dashboard Khảo thí (T-17), lưu payload nhạy cảm không cần thiết.

## Đầu vào đã có
- Audit schema T-03.
- Actor/role/scope T-04.
- Các action bắt buộc tại PRD và CONVENTIONS.md.

## Việc phải làm
1. Định nghĩa event contract gồm actor/service, action, object ID, timestamp, outcome, correlation ID và metadata tối thiểu.
2. Tạo recorder append-only, từ chối update/delete qua application interface.
3. Hỗ trợ query theo transcript/actor/action/time với phân trang.
4. Không ghi PDF, token, PIN hoặc dữ liệu không cần thiết.
5. Công bố contract để module khác phát event mà không sửa package này.

## Quy ước bắt buộc
- Chỉ sửa đường dẫn trong touches của card đang làm. Không sửa card khác để đổi phụ thuộc/trạng thái.
- Không hard-code bí mật, URL CA/OCSP/CRL/TSA, issuer OIDC, thông tin MinIO/NAS hoặc Google Drive; dùng cấu hình môi trường được tài liệu hóa.
- T-11 sở hữu contract audit và package audit.
- Audit failure cho hành động trọng yếu theo fail-closed policy T-01.
- Không tuyên bố chống sửa ở tầng ứng dụng nếu storage/DB chưa hỗ trợ.

## Checklist đầu ra
- [ ] Backend compile/test theo CONVENTIONS.md.
- [ ] Audit API/repository tests xanh.
- [ ] Contract đủ để feature phát event và Khảo thí tra cứu có scope.
- [ ] Không đụng file ngoài touches.
- [ ] Cập nhật status: review và finished_at trong frontmatter card này.
- [ ] Ghi 3–5 dòng “Đã làm gì” vào cuối card.

## Test phải viết
- Ghi event lưu đúng actor/object/time/outcome.
- Event cũ không update/delete được qua API/repository thường.
- Actor ngoài scope không xem được event.
- Secret/token/PDF bytes không nằm trong serialized event.

## Định nghĩa "xong"
Module nghiệp vụ dùng được contract ổn định để ghi hành động nhạy cảm và Khảo thí tra cứu đúng scope.

## Cạm bẫy đã biết
Không ghi audit bất đồng bộ theo cách mất event khi process dừng; dùng transaction/outbox theo T-01.

## Đã làm gì
(agent điền khi xong)

