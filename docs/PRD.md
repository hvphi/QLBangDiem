Dưới đây là phần **Định nghĩa Kiến trúc Phần mềm (Software Architecture)** chi tiết dành cho trang web Quản lý Bảng điểm Điện tử Ký số, được biên soạn dưới góc nhìn của một **Tech Lead / Full-Role GEM**.

Tài liệu này tích hợp toàn bộ các yêu cầu từ file `HuongDan ky so.docx` (sử dụng vSignPDF/VGCA, quy trình 4 bước, cấu trúc lưu trữ `Năm học -> Học kỳ -> Khoa -> Mã lớp học phần`, và cơ chế tự động xác thực) kết hợp với các tiêu chuẩn an toàn thông tin khắt khe của Bộ Công an / Ban Cơ yếu Chính phủ.

Nội dung dưới đây đã được định dạng chuẩn để **bổ sung trực tiếp vào PRD thành Mục 8**.

---

# BỔ SUNG VÀO TÀI LIỆU YÊU CẦU SẢN PHẨM (PRD)

## 8. KIẾN TRÚC HỆ THỐNG (SOFTWARE ARCHITECTURE) - *Tác giả: Tech Lead*

### 8.1. Mô hình Kiến trúc Tổng thể (Overall System Architecture)

Hệ thống sử dụng kiến trúc **Modular Monolith** (hoặc **Microservices nhẹ**) kết hợp với **DMZ-isolated Verification Engine** để đảm bảo khả năng cô lập an ninh, dễ bảo trì và dễ triển khai nội bộ tại nhà trường (On-premise / Hybrid Cloud).

```
[ FRONTEND LAYER ]
├── Web Portal cho Giảng viên / Trưởng khoa / Khảo thí (React.js / Next.js)
└── WebPKI Native Bridge / vSignPDF Local Agent (Giao tiếp USB Token)
       │
       ▼ (HTTPS / TLS 1.3)
[ API GATEWAY & SECURITY LAYER ]
├── Nginx / Kong Gateway (Rate Limiting, WAF, Authentication JWT/OAuth2)
       │
       ├───────────────────────────────┐
       ▼                               ▼
[ CORE BACKEND SERVICES ]     [ PKI & SIGNATURE ENGINE ]
├── Grade & Workflow Service   ├── Certificate Validation (OCSP/CRL VGCA/BCA)
├── Storage & Folder Service   ├── PDF Hash & Digital Signature Processor (PAdES)
└── Audit & Log Service        └── TSA Time-stamp Client (RFC 3161)
       │                               │
       ▼                               ▼
[ DATA STORAGE LAYER ]        [ CRYPTO / EXTERNAL INFRA ]
├── PostgreSQL (Relational DB) ├── VGCA / BCA Root CA Server
├── Redis (Cache & Session)    ├── Timestamp Authority (TSA BCA) Server
└── MinIO / Local NAS S3       └── External Backup (Google Drive Enterprise / AWS S3)
    (Quy hoạch cây thư mục)

```

---

### 8.2. Các Thành phần Kiến trúc Chính (Architectural Components)

#### 1. WebPKI Integration Agent / Browser Extension Component:

* Phụ trách giao tiếp giữa trình duyệt Web và **USB Token chuyên dùng của Bộ Công an / Ban Cơ yếu Chính phủ (VGCA)** cắm tại máy Giảng viên / Trưởng khoa thông qua chuẩn **PKCS#11**.
* Trường hợp ký ngoài Web qua phần mềm **vSignPDF**: Cung cấp API Handler tiếp nhận file PDF đã ký và trích xuất Metadata chữ ký tự động.



#### 2. Digital Signature & Verification Engine (Core Crypto Engine):

* **Signature Embedding:** Thực hiện ký số chuẩn **PAdES-LTV (PDF Advanced Electronic Signatures - Long Term Validation)**, đảm bảo nhúng đầy đủ chuỗi chứng thư và thông tin thu hồi (CRL/OCSP) vào file PDF.
* **Auto-Validation Engine:** Đảm nhận việc kiểm tra tính toàn vẹn file PDF ngay khi upload. Tự động phân tích các Signature Layer:


* *Chữ ký 1 (Giảng viên):* Xóa bỏ nhu cầu kiểm tra thủ công bằng vSignPDF tại phòng Khảo thí.


* *Chữ ký 2 (Trưởng khoa):* Xác thực tính đầy đủ của 02 chữ ký.


* *Integrity Check:* Phát hiện vết cắt đứt chuỗi Hash (nếu file bị scan lại, in ra rồi scan, hoặc chỉnh sửa byte).




* **TSA Client:** Kết nối trực tiếp với máy chủ Cấp dấu thời gian **TSA (Time-Stamp Authority)** của Ban Cơ yếu/Bộ Công an theo chuẩn **RFC 3161**.

#### 3. Automated Storage & Partitioning Service:

* Tự động quản lý vị trí lưu trữ file theo đúng quy định tại **Mục 3 - Hướng dẫn ký số**:


```text
/StorageRoot
  ├── /2025-2026 (Năm học)
  │     └── /Hocky_1 (Học kỳ)
  │           └── /Khoa_CNTT (Khoa)
  │                 ├── MaLHP_TenMon_TenGiangVien.pdf
  │                 └── ...
```[cite: 1]

```


* Tự động kích hoạt cơ chế **Write-Once-Read-Many (WORM)** / **Set-Readonly** đối với các thư mục đã quá hạn nộp điểm.



---

### 8.3. Sơ đồ Luồng Dữ liệu & Xử lý Chữ ký số (Sequence Diagram)

```
Giảng viên (GV)    Trưởng khoa (TK)      Web Portal         Crypto Engine       Storage & NAS
     │                   │                   │                   │                   │
     │─ 1. Xuất PDF ────►│                   │                   │                   │
     │   & Ký số GV      │                   │                   │                   │
     │─ 2. Upload PDF ──►│                   │                   │                   │
     │  `MaLHP_...pdf`   │                   │── 3. Validate ───►│                   │
     │                   │                   │      Chữ ký GV    │                   │
     │                   │                   │◄─ 4. Hợp lệ ──────│                   │
     │                   │                   │                                       │
     │                   │◄─ 5. Notification │                                       │
     │                   │   file chờ duyệt  │                                       │
     │                   │                   │                                       │
     │                   │── 6. Ký số TK ───►│                                       │
     │                   │   (Cấp 2)         │── 7. Validate ───►│                   │
     │                   │                   │      2 chữ ký     │                   │
     │                   │                   │◄─ 8. Hợp lệ ──────│                   │
     │                   │                   │                                       │
     │                   │                   │── 9. Auto-routing & Store ───────────►│
     │                   │                   │   `/NamHoc/HocKy/Khoa/MaLHP.pdf`      │
     │                   │                   │                                       │
     │                   │                   │── 10. Sync Backup ───────────────────►│
     │                   │                   │    (Google Drive / Cloud)             │

```

---

### 8.4. Cấu trúc Cơ sở Dữ liệu Core (Key Database Schema)

#### Bảng `transcripts` (Quản lý Bảng điểm điện tử)

```sql
CREATE TABLE transcripts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_class_code VARCHAR(50) NOT NULL, -- Mã lớp học phần (MaLHP)
    subject_name VARCHAR(255) NOT NULL,     -- Tên môn học
    lecturer_id UUID NOT NULL,               -- Giảng viên giảng dạy
    department_id UUID NOT NULL,             -- Khoa quản lý
    academic_year VARCHAR(20) NOT NULL,      -- Năm học (VD: 2025-2026)
    semester VARCHAR(10) NOT NULL,           -- Học kỳ (VD: HK1)
    file_name VARCHAR(255) NOT NULL,        -- Quy chuẩn: MaLHP_TenMon_TenGiangVien.pdf
    file_path TEXT NOT NULL,                 -- Đường dẫn lưu trữ vật lý trên NAS/MinIO
    file_hash_sha256 VARCHAR(64) NOT NULL,   -- Chuỗi Hash SHA-256 để kiểm tra chống biến đổi file
    status VARCHAR(30) NOT NULL DEFAULT 'LECTURER_SIGNED', 
    -- Status: LECTURER_SIGNED -> DEPT_APPROVED -> ARCHIVED -> REJECTED
    is_readonly BOOLEAN DEFAULT FALSE,       -- Cờ khóa thư mục/file sau hạn nộp
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

```

#### Bảng `digital_signatures` (Nhật ký Xác thực Chữ ký số - Non-repudiation Audit)

```sql
CREATE TABLE digital_signatures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transcript_id UUID REFERENCES transcripts(id) ON DELETE CASCADE,
    signer_type VARCHAR(20) NOT NULL,        -- 'LECTURER' (Giảng viên) hoặc 'DEPT_HEAD' (Trưởng khoa)
    signer_name VARCHAR(150) NOT NULL,       -- Tên người ký ghi nhận từ Certificate
    certificate_serial VARCHAR(100) NOT NULL,-- Serial Number của Certificate BCA/VGCA
    issuer_dn TEXT NOT NULL,                 -- Đơn vị cấp CA (VGCA / Police CA)
    signed_at TIMESTAMP WITH TIME ZONE,      -- Thời gian ký
    timestamp_tsa_token TEXT,                -- Dữ liệu TSA RFC 3161 chứng minh thời gian ký
    validation_status VARCHAR(20) NOT NULL,  -- 'VALID', 'INVALID', 'REVOKED', 'EXPIRED'
    validated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

```

---

### 8.5. Tiêu chuẩn Kỹ thuật & Công nghệ (Tech Stack Selection)

| Hạng mục | Công nghệ lựa chọn | Lý do đề xuất (Góc nhìn Tech Lead) |
| --- | --- | --- |
| **Backend Core** | **Java (Spring Boot 3.x) / Go** | Java có hệ sinh thái thư viện xử lý Cryptography/PDF (BouncyCastle, Apache PDFBox, iText) mạnh mẽ và chuẩn xác nhất cho PAdES. |
| **Crypto Library** | **Bouncy Castle + Apache PDFBox** | Hỗ trợ bóc tách PKCS#7/CMS Signature, kiểm tra Revocation List (CRL/OCSP) từ BCA và xử lý LTV. |
| **Frontend** | **React.js / Next.js (TypeScript)** | Render UI mượt mà, hỗ trợ tốt tích hợp WebSocket/WebPKI Extension để giao tiếp USB Token. |
| **Database** | **PostgreSQL 16+** | Tuân thủ ACID nghiêm ngặt, hỗ trợ JSONB lưu vết Metadata chữ ký, hiệu năng cao. |
| **Object Storage** | **MinIO / Local NAS (On-Premise)** | Thay thế Google Drive local, hỗ trợ đặt quyền Policy Read-Only cấp hạ tầng và phân tầng thư mục chuẩn.

 |
| **Sync Backup** | **Rclone / Google Drive API Engine** | Tự động hóa đồng bộ backup định kỳ từ MinIO sang Cloud/Google Drive theo quy định tại Mục 3.

 |

---

### 8.6. Chiến lược An toàn Thông tin & Dự phòng Lỗi (Resilience & Security Strategy)

1. **Bảo vệ Tính toàn vẹn Chữ ký số (Signature Preservation):**
* Tệp PDF sau khi Giảng viên ký không bao giờ được ghi đè hay biến đổi cấu trúc Byte gốc. Chữ ký của Trưởng khoa sẽ được append dưới dạng **Incremental Save (PDF Multi-signature)** để giữ nguyên hiệu lực chữ ký 1.




2. Cơ chế Dự phòng & Sao lưu (Backup & Recovery):


* **Primary Storage:** Lưu trên MinIO Cluster (On-premise NAS) theo đường dẫn `Năm học/Học kỳ/Khoa/MaLHP.pdf`.


* **Secondary Backup (Cloud):** Đẩy dữ liệu mã hóa lên Google Drive Enterprise của Trường / Ổ cứng dự phòng định kỳ lúc 00:00 mỗi ngày.




3. **Audit Trail & Non-repudiation (Chống chối bỏ):**
* Mọi thao tác *Upload, Verify, Ký duyệt, Khóa quyền Edit, Download* đều được ghi nhận vào bảng Log bất biến (Append-only) phục vụ thanh tra đào tạo.