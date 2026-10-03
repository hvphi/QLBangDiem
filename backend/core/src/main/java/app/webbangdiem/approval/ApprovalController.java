package app.webbangdiem.approval;

import app.webbangdiem.identity.Actor;
import app.webbangdiem.identity.ActorContext;
import app.webbangdiem.notification.WorkflowNotificationEvent;
import app.webbangdiem.storage.LocalImmutableStorage;
import app.webbangdiem.storage.StoredObject;
import app.webbangdiem.submission.SubmissionController;
import app.webbangdiem.submission.TranscriptSummary;
import app.webbangdiem.submission.VerificationResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {
    private final JdbcTemplate jdbc;
    private final SubmissionController submissions;

    public ApprovalController(JdbcTemplate jdbc, SubmissionController submissions) {
        this.jdbc = jdbc; this.submissions = submissions;
    }

    @GetMapping("/queue")
    public List<TranscriptSummary> queue() {
        Actor actor = ActorContext.requireRole("DEPT_HEAD", "EXAMINATION", "ADMIN");
        String sql = """
                SELECT x.id FROM transcripts x JOIN course_classes c ON c.id=x.course_class_id
                WHERE x.status='LECTURER_SIGNED'
                """;
        if (actor.hasRole("DEPT_HEAD")) sql += " AND c.department_id=?";
        sql += " ORDER BY x.created_at ASC LIMIT 250";
        List<String> ids = actor.hasRole("DEPT_HEAD")
                ? jdbc.query(sql, (rs, row) -> rs.getString(1), actor.departmentId())
                : jdbc.query(sql, (rs, row) -> rs.getString(1));
        return ids.stream().map(submissions::summary).toList();
    }

    @PostMapping(path = "/{id}/signed-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public TranscriptSummary approve(@PathVariable String id, @RequestPart("file") MultipartFile file,
                                    @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) throws Exception {
        Actor actor = ActorContext.requireRole("DEPT_HEAD");
        TranscriptSummary previous = submissions.summaryScoped(actor, id);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var previousRevision = jdbc.query("SELECT id FROM transcripts WHERE supersedes_transcript_id=? AND status='ARCHIVED'",
                    (rs, row) -> rs.getString(1), id);
            if (!previousRevision.isEmpty()) return submissions.summary(previousRevision.getFirst());
        }
        if (!"LECTURER_SIGNED".equals(previous.status())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Hồ sơ không còn ở trạng thái chờ duyệt.");
        byte[] bytes = file.getBytes();
        VerificationResult result = submissions.verifier().verify(bytes, file.getOriginalFilename());
        if (!LocalImmutableStorage.sha256(bytes).equalsIgnoreCase(result.sha256())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Checksum trả về từ dịch vụ xác minh không khớp.");
        }
        if (!"VALID".equals(result.status()) || result.signatureCount() < 2) {
            submissions.audit().record(actor, "APPROVAL_VERIFY", "transcript", id, result.status(),
                    Map.of("sha256", result.sha256(), "signatureCount", result.signatureCount(), "reason", result.reason()));
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không thể lưu duyệt: cần hai chữ ký hợp lệ (" + result.status() + ", " + result.reason() + ").");
        }
        var courseClass = submissions.catalog().get(previous.courseClassId());
        int revision = jdbc.queryForObject("SELECT COALESCE(MAX(revision), 0) + 1 FROM transcripts WHERE course_class_id=?", Integer.class, previous.courseClassId());
        StoredObject stored = submissions.storage().store(bytes, courseClass, revision);
        if (!stored.sha256().equalsIgnoreCase(result.sha256())) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Checksum thay đổi khi lưu.");
        String newId = UUID.randomUUID().toString();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String lecturerId = jdbc.queryForObject("SELECT lecturer_id FROM transcripts WHERE id=?", String.class, id);
        jdbc.update("""
                INSERT INTO transcripts(id, course_class_id, lecturer_id, revision, file_name, object_key,
                  file_hash_sha256, file_size, status, supersedes_transcript_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'DEPT_APPROVED', ?, ?, ?)
                """, newId, previous.courseClassId(), lecturerId, revision, previous.fileName(), stored.objectKey(),
                stored.sha256(), stored.size(), id, now, now);
        submissions.saveSignatures(newId, result.signatures(), "LECTURER");
        submissions.audit().record(actor, "DEPT_APPROVED", "transcript", newId, "SUCCESS", Map.of("supersedes", id, "sha256", stored.sha256()));
        jdbc.update("UPDATE transcripts SET status='ARCHIVED', updated_at=? WHERE id=? AND status='DEPT_APPROVED'", now, newId);
        submissions.audit().record(actor, "TRANSCRIPT_ARCHIVED", "transcript", newId, "SUCCESS", Map.of("revision", revision));
        submissions.events().publishEvent(new WorkflowNotificationEvent("TRANSCRIPT_APPROVED", previousLecturerId(id), newId,
                "Bảng điểm đã được duyệt và lưu trữ: " + previous.courseClassCode()));
        return submissions.summary(newId);
    }

    @PostMapping("/{id}/reject")
    @Transactional
    public TranscriptSummary reject(@PathVariable String id, @Valid @RequestBody RejectRequest request) {
        Actor actor = ActorContext.requireRole("DEPT_HEAD");
        TranscriptSummary summary = submissions.summaryScoped(actor, id);
        if (!"LECTURER_SIGNED".equals(summary.status())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Hồ sơ không còn ở trạng thái chờ duyệt.");
        String reason = request.reason().strip();
        if (reason.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần ghi lý do từ chối.");
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbc.update("UPDATE transcripts SET status='REJECTED', rejection_reason=?, updated_at=? WHERE id=?", reason, now, id);
        submissions.audit().record(actor, "TRANSCRIPT_REJECTED", "transcript", id, "SUCCESS", Map.of("reason", reason));
        submissions.events().publishEvent(new WorkflowNotificationEvent("TRANSCRIPT_REJECTED", previousLecturerId(id), id,
                "Bảng điểm " + summary.courseClassCode() + " bị từ chối: " + reason));
        return submissions.summary(id);
    }

    private String previousLecturerId(String id) {
        return jdbc.queryForObject("SELECT lecturer_id FROM transcripts WHERE id=?", String.class, id);
    }
}
