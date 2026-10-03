---
id: T-19
title: Đóng gói triển khai on-premise
status: in_progress
model: codex
effort: high
depends_on: ["T-02", "T-06", "T-07", "T-08", "T-12", "T-13", "T-18"]
touches:
  - "infra/deploy/**"
  - "docs/ops/deployment.md"
  - "docs/ops/incident-response.md"
prd_refs: ["§8.1", "§8.5", "§8.6"]
owner: null
started_at: 2026-09-28
finished_at: null
---

# T-19 · Đóng gói triển khai on-premise

## Mục tiêu
Đóng gói hệ thống để triển khai nội bộ, cấu hình tách môi trường và vận hành được health, log, backup, nâng cấp/rollback.

## Ngữ cảnh cần biết
PRD đặt mục tiêu on-premise/hybrid với core, verification cô lập, PostgreSQL, Redis, MinIO/NAS, gateway và backup.

## Phạm vi
**Trong:** Production topology/container/manifests, secret references, health/readiness, migration/rollback và runbooks.

**Ngoài:** Kết nối thật, deploy lên hạ tầng trường, production private keys/certs, dev config T-02.

## Đầu vào đã có
- Topology T-01; skeleton T-02; gateway T-13; backup T-18.
- Runtime contracts T-06/T-07/T-08/T-12.

## Việc phải làm
1. Mô tả vùng mạng portal/core, DMZ verification, storage và outbound CA/TSA/backup.
2. Tạo manifest production mẫu không nhúng secrets; giới hạn container/network/filesystem.
3. Thiết lập health/readiness, correlation logging, resource limits và cảnh báo tối thiểu.
4. Viết deploy/upgrade/rollback/restore/rotate secret và xử lý CA/TSA unavailable.
5. Kiểm tra verification không public và config không chứa endpoint/bí mật thật.

## Quy ước bắt buộc
- Chỉ sửa đường dẫn trong touches của card đang làm. Không sửa card khác để đổi phụ thuộc/trạng thái.
- Không hard-code bí mật, URL CA/OCSP/CRL/TSA, issuer OIDC, thông tin MinIO/NAS hoặc Google Drive; dùng cấu hình môi trường được tài liệu hóa.
- Không sửa infra/dev, gateway hay backup; tham chiếu output các card đó.
- Không đưa production secret vào repo/image.
- Tuân thủ phân vùng mạng/fail-closed T-01.

## Checklist đầu ra
- [ ] Config/container lint theo CONVENTIONS.md xanh.
- [ ] Smoke test staging giả lập và health checks xanh.
- [ ] Runbook deploy/rollback/restore đầy đủ.
- [ ] Không đụng file ngoài touches.
- [ ] Cập nhật status: review và finished_at trong frontmatter card này.
- [ ] Ghi 3–5 dòng “Đã làm gì” vào cuối card.

## Test phải viết
- Services khởi động/readiness đúng dependency.
- Verification chỉ nhận traffic core/service identity.
- Thiếu secret bắt buộc làm deploy fail rõ, không dùng default yếu.
- Rollback/restore không xóa transcript/PDF đã ký.

## Định nghĩa "xong"
Vận hành dựng được môi trường mẫu, kiểm tra health, upgrade và khôi phục theo runbook.

## Cạm bẫy đã biết
Không đặt verification trong trust zone public; không dùng image latest hoặc container privileged.

## Đã làm gì
(agent điền khi xong)

