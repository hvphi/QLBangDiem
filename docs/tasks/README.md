# Task cards — Web quản lý bảng điểm ký số

Thực hiện theo depends_on; chỉ chạy song song khi phụ thuộc đã xong. Mọi card mới ở trạng thái todo. Mỗi card sở hữu đúng touches; không sửa card khác để đổi trạng thái/phụ thuộc. Khi cần thay file dùng chung, yêu cầu chủ sở hữu file cập nhật trước.

## Danh sách và ranh giới sở hữu

| ID | Task | Phụ thuộc | Vùng sở hữu chính |
|---|---|---|---|
| T-01 | Chốt kiến trúc và quy ước | — | docs/architecture/DECISIONS.md, docs/tasks/CONVENTIONS.md |
| T-02 | Dựng workspace và CI | T-01 | manifest gốc, infra/dev, CI |
| T-03 | Schema và migration miền nghiệp vụ | T-02 | migration, domain model dùng chung |
| T-04 | Đăng nhập và phân quyền | T-03 | module identity/access |
| T-05 | Danh mục học vụ và hạn nộp | T-03, T-04 | module catalog |
| T-06 | Engine kiểm tra chữ ký | T-01, T-02 | dịch vụ verification cô lập |
| T-07 | Lưu trữ, định tuyến và khóa sửa | T-03, T-05, T-11 | module storage |
| T-08 | Cầu nối ký số USB Token | T-01, T-02 | signing-bridge |
| T-09 | Nộp bảng điểm của giảng viên | T-04, T-05, T-06, T-07, T-11 | module submission |
| T-10 | Duyệt và ký cấp trưởng khoa | T-08, T-09 | module approval |
| T-11 | Audit trail bất biến | T-03, T-04 | module audit và hợp đồng audit |
| T-12 | Thông báo quy trình | T-09, T-10 | module notification |
| T-13 | API gateway và bảo vệ biên | T-02, T-04 | infra/gateway |
| T-14 | Khung giao diện và đăng nhập | T-02, T-04 | app shell, auth UI dùng chung |
| T-15 | Cổng giảng viên | T-08, T-09, T-14 | route/feature giảng viên |
| T-16 | Cổng trưởng khoa | T-08, T-10, T-12, T-14 | route/feature trưởng khoa |
| T-17 | Cổng Khảo thí | T-05, T-11, T-12, T-14 | route/feature Khảo thí |
| T-18 | Sao lưu và khôi phục | T-07 | infra/backup, runbook |
| T-19 | Triển khai on-premise | T-02, T-06, T-07, T-08, T-12, T-13, T-18 | infra/deploy, runbook |
| T-20 | Kiểm thử chấp nhận và gia cố | T-09, T-10, T-12, T-15, T-16, T-17, T-19 | tests/integration, frontend/e2e, acceptance |

## Các đợt chạy

1. T-01 rồi T-02.
2. Sau T-02, T-03, T-06 và T-08 có thể chạy song song. T-04 chờ T-03; T-13 và T-14 chờ T-04.
3. Sau T-04/T-03, T-05 và T-11 có thể chạy song song. T-07 chờ T-05 và T-11; T-09 chờ T-05, T-06, T-07 và T-11.
4. T-10 theo sau T-09; T-12 theo sau T-10. T-15/T-16/T-17 chỉ bắt đầu khi API/contract tương ứng và T-14 xong.
5. T-18 chạy sau T-07. T-19 chờ các dịch vụ cần triển khai. T-20 là tích hợp cuối.

T-02 là chủ duy nhất của mọi build manifest/package và CI; T-03 là chủ migration/domain model dùng chung; T-11 sở hữu contract audit; T-14 sở hữu shell/auth UI. Mỗi feature có source, contract API, tests và route riêng. Nếu hai card có touches giao nhau, sửa phụ thuộc/sở hữu trong README trước khi chạy song song.

## Tài liệu đầu vào

- PRD: docs/PRD.md.
- Template: docs/tasks/_TEMPLATE.md.
- Quy ước chung: docs/tasks/CONVENTIONS.md; T-01 hoàn thiện trước khi bắt đầu code.
- PRD nhắc hướng dẫn ký số tại Mục 3; T-01 đối chiếu hướng dẫn có sẵn, chỉ đưa yêu cầu có căn cứ vào card và ghi rõ điểm còn thiếu.
