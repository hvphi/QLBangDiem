package app.webbangdiem.submission;

import app.webbangdiem.audit.AuditService;
import app.webbangdiem.catalog.CatalogService;
import app.webbangdiem.catalog.CourseClass;
import app.webbangdiem.identity.Actor;
import app.webbangdiem.identity.ActorContext;
import app.webbangdiem.notification.WorkflowNotificationEvent;
import app.webbangdiem.storage.DocumentStorage;
import app.webbangdiem.storage.LocalImmutableStorage;
import app.webbangdiem.storage.StoredObject;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {
    private final JdbcTemplate jdbc;
    private final CatalogService catalog;
    private final VerificationClient verifier;
    private final DocumentStorage storage;
    private final AuditService audit;
    private final ApplicationEventPublisher events;

    public SubmissionController(JdbcTemplate jdbc, CatalogService catalog, VerificationClient verifier,
                                DocumentStorage storage, AuditService audit, ApplicationEventPublisher events) {
        this.jdbc = jdbc; this.catalog = catalog; this.verifier = verifier; this.storage = storage;
        this.audit = audit; this.events = events;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TranscriptSummary> upload(@RequestParam String courseClassId,
                                                    @RequestPart("file") MultipartFile file,
                                                    @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) throws Exception {
        Actor actor = ActorContext.requireRole("LECTURER");
        CourseClass courseClass = catalog.get(courseClassId);
        if (!catalog.canAccess(actor, courseClassId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Giảng viên không phụ trách lớp học phần này.");
        if (!catalog.isDeadlineOpen(courseClass, actor)) throw new ResponseStatusException(HttpStatus.LOCKED, "Đã hết hạn nộp bảng điểm cho lớp học phần này.");
        byte[] bytes = file.getBytes();
        String fileName = safeName(file.getOriginalFilename(), courseClass, actor.displayName());
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = jdbc.query("SELECT id, course_class_id, file_hash_sha256 FROM transcripts WHERE lecturer_id=? AND idempotency_key=?",
                    (rs, row) -> new String[]{rs.getString(1), rs.getString(2), rs.getString(3)}, actor.id(), idempotencyKey);
            if (!existing.isEmpty()) {
                if (!existing.getFirst()[1].equals(courseClassId)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key đã được dùng cho lớp khác.");
                if (!existing.getFirst()[2].equalsIgnoreCase(LocalImmutableStorage.sha256(bytes))) throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key đã được dùng cho một PDF khác.");
                return ResponseEntity.ok(summary(existing.getFirst()[0]));
            }
        }
        VerificationResult result = verifier.verify(bytes, fileName);
        if (!LocalImmutableStorage.sha256(bytes).equalsIgnoreCase(result.sha256())) {
            result = new VerificationResult("INVALID", LocalImmutableStorage.sha256(bytes), result.signatureCount(), "HASH_MISMATCH", result.signatures());
        }
        if (!"VALID".equals(result.status())) {
            audit.record(actor, "SUBMISSION_VERIFY", "course_class", courseClassId, result.status(),
                    Map.of("fileName", fileName, "sha256", result.sha256(), "reason", result.reason()));
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    verificationFailure(result.status(), result.reason()));
        }
        int revision = jdbc.queryForObject("SELECT COALESCE(MAX(revision), 0) + 1 FROM transcripts WHERE course_class_id=?", Integer.class, courseClassId);
        StoredObject stored = storage.store(bytes, courseClass, revision);
        if (!stored.sha256().equalsIgnoreCase(result.sha256())) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Checksum thay đổi khi lưu.");
        String id = UUID.randomUUID().toString();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbc.update("""
                INSERT INTO transcripts(id, course_class_id, lecturer_id, revision, file_name, object_key,
                  file_hash_sha256, file_size, status, idempotency_key, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'LECTURER_SIGNED', ?, ?, ?)
                """, id, courseClassId, actor.id(), revision, fileName, stored.objectKey(), stored.sha256(),
                stored.size(), blankToNull(idempotencyKey), now, now);
        saveSignatures(id, result.signatures(), "LECTURER");
        audit.record(actor, "SUBMISSION_UPLOADED", "transcript", id, "SUCCESS", Map.of("sha256", stored.sha256(), "revision", revision));
        audit.record(actor, "SIGNATURE_VERIFIED", "transcript", id, "VALID", Map.of("signatureCount", result.signatureCount()));
        String departmentHeadId = jdbc.queryForObject("SELECT id FROM app_users WHERE department_id=? AND role='DEPT_HEAD'", String.class, actor.departmentId());
        events.publishEvent(new WorkflowNotificationEvent("SUBMISSION_RECEIVED", departmentHeadId, id,
                "Có bảng điểm mới chờ duyệt: " + courseClass.courseClassCode()));
        return ResponseEntity.status(HttpStatus.CREATED).body(summary(id));
    }

    @GetMapping
    public List<TranscriptSummary> list() {
        Actor actor = ActorContext.current();
        StringBuilder sql = new StringBuilder(baseSelect()).append(" WHERE 1=1");
        var args = new java.util.ArrayList<Object>();
        if (actor.hasRole("LECTURER")) { sql.append(" AND x.lecturer_id=?"); args.add(actor.id()); }
        else if (actor.hasRole("DEPT_HEAD")) { sql.append(" AND c.department_id=?"); args.add(actor.departmentId()); }
        else if (!actor.hasRole("EXAMINATION") && !actor.hasRole("ADMIN")) return List.of();
        sql.append(" ORDER BY x.created_at DESC LIMIT 250");
        return jdbc.query(sql.toString(), this::mapSummary, args.toArray());
    }

    @GetMapping("/{id}")
    public TranscriptSummary get(@PathVariable String id) { return summaryScoped(ActorContext.current(), id); }

    @GetMapping("/{id}/signatures")
    public List<SignatureRecord> signatures(@PathVariable String id) {
        summaryScoped(ActorContext.current(), id);
        return jdbc.query("SELECT signer_type, signer_name, certificate_serial, issuer_dn, signed_at, validation_status, validation_reason FROM digital_signatures WHERE transcript_id=? ORDER BY validated_at",
                (rs, row) -> new SignatureRecord(rs.getString("signer_type"), rs.getString("signer_name"),
                        rs.getString("certificate_serial"), rs.getString("issuer_dn"),
                        rs.getObject("signed_at", OffsetDateTime.class), rs.getString("validation_status"),
                        rs.getString("validation_reason")), id);
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> download(@PathVariable String id) {
        Actor actor = ActorContext.current();
        TranscriptSummary summary = summaryScoped(actor, id);
        var row = jdbc.queryForMap("SELECT object_key, file_hash_sha256 FROM transcripts WHERE id=?", id);
        byte[] bytes = storage.read((String) row.get("OBJECT_KEY"), (String) row.get("FILE_HASH_SHA256"));
        audit.record(actor, "TRANSCRIPT_DOWNLOADED", "transcript", id, "SUCCESS", Map.of("sha256", summary.sha256()));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline().filename(summary.fileName(), StandardCharsets.UTF_8).build());
        headers.set("X-Content-Type-Options", "nosniff");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    public TranscriptSummary summary(String id) {
        return jdbc.queryForObject(baseSelect() + " WHERE x.id=?", this::mapSummary, id);
    }

    public TranscriptSummary summaryScoped(Actor actor, String id) {
        TranscriptSummary summary = summary(id);
        if (!catalog.canAccess(actor, summary.courseClassId()) && !actor.hasRole("EXAMINATION") && !actor.hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền xem bảng điểm này.");
        }
        return summary;
    }

    public JdbcTemplate jdbc() { return jdbc; }
    public CatalogService catalog() { return catalog; }
    public VerificationClient verifier() { return verifier; }
    public DocumentStorage storage() { return storage; }
    public AuditService audit() { return audit; }
    public ApplicationEventPublisher events() { return events; }

    public void saveSignatures(String transcriptId, List<SignatureCheck> signatures, String firstSignerType) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        for (int i = 0; i < signatures.size(); i++) {
            SignatureCheck signature = signatures.get(i);
            String signerType = i == 0 ? firstSignerType : "DEPT_HEAD";
            jdbc.update("""
                    INSERT INTO digital_signatures(id, transcript_id, signer_type, signer_name, certificate_serial,
                      issuer_dn, signed_at, validation_status, validation_reason, validated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, UUID.randomUUID().toString(), transcriptId, signerType, signature.signerName(),
                    signature.certificateSerial(), signature.issuer(), signature.signedAt(), signature.status(), signature.reason(), now);
        }
    }

    private static String verificationFailure(String status, String reason) {
        String message = switch (reason) {
            case "NO_SIGNATURE" -> "PDF chưa được ký số.";
            case "INSUFFICIENT_SIGNATURES" -> "PDF chưa đủ 2 chữ ký số theo quy trình.";
            case "SIGNATURE_INTEGRITY_INVALID" -> "Chữ ký số không hợp lệ hoặc PDF đã bị thay đổi sau khi ký.";
            case "PDF_MALFORMED_OR_SIGNATURE_UNREADABLE" -> "Không đọc được cấu trúc chữ ký trong PDF.";
            case "VERIFICATION_TIMEOUT", "VERIFIER_PROCESS_FAILED", "PYHANKO_BRIDGE_UNAVAILABLE",
                 "VERIFICATION_INTERRUPTED" -> "Dịch vụ xác minh chữ ký số hiện không khả dụng. Vui lòng thử lại sau.";
            default -> "Không thể ghi nhận bảng điểm: " + status + " (" + reason + ").";
        };
        return message + " [" + reason + "]";
    }

    private static String baseSelect() {
        return """
                SELECT x.id, x.course_class_id, c.course_class_code, c.subject_name,
                       l.display_name AS lecturer_name, d.name AS department_name,
                       t.academic_year, t.semester, x.file_name, x.file_hash_sha256, x.file_size,
                       x.status, x.revision, x.created_at, x.rejection_reason,
                       (SELECT COUNT(*) FROM digital_signatures s WHERE s.transcript_id=x.id) AS signature_count
                FROM transcripts x JOIN course_classes c ON c.id=x.course_class_id
                JOIN app_users l ON l.id=x.lecturer_id JOIN departments d ON d.id=c.department_id
                JOIN academic_terms t ON t.id=c.academic_term_id
                """;
    }

    private TranscriptSummary mapSummary(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new TranscriptSummary(rs.getString("id"), rs.getString("course_class_id"), rs.getString("course_class_code"),
                rs.getString("subject_name"), rs.getString("lecturer_name"), rs.getString("department_name"),
                rs.getString("academic_year"), rs.getString("semester"), rs.getString("file_name"),
                rs.getString("file_hash_sha256"), rs.getLong("file_size"), rs.getString("status"),
                rs.getInt("revision"), rs.getObject("created_at", OffsetDateTime.class), rs.getString("rejection_reason"),
                rs.getInt("signature_count"));
    }

    private static String safeName(String original, CourseClass courseClass, String lecturerName) {
        String subject = courseClass.subjectName().replaceAll("[\\p{Cntrl}/\\\\:*?\"<>|]", "_").replaceAll("\\s+", "_");
        String lecturer = lecturerName.replaceAll("[\\p{Cntrl}/\\\\:*?\"<>|]", "_").replaceAll("\\s+", "_");
        String preferred = courseClass.courseClassCode() + "_" + subject + "_" + lecturer + ".pdf";
        return preferred.length() <= 240 ? preferred : preferred.substring(0, 236) + ".pdf";
    }

    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.strip(); }
}
