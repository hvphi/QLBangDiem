package app.webbangdiem.audit;

import app.webbangdiem.identity.Actor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final HttpServletRequest request;

    public AuditService(JdbcTemplate jdbc, ObjectMapper mapper, HttpServletRequest request) {
        this.jdbc = jdbc; this.mapper = mapper; this.request = request;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized AuditEvent record(Actor actor, String action, String objectType, String objectId,
                                          String outcome, Map<String, ?> metadata) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS);
        String id = UUID.randomUUID().toString();
        String previousHash = jdbc.query("SELECT event_hash FROM audit_events ORDER BY event_seq DESC LIMIT 1",
                rs -> rs.next() ? rs.getString(1) : null);
        String safeMetadata;
        try { safeMetadata = mapper.writeValueAsString(metadata == null ? Map.of() : metadata); }
        catch (JsonProcessingException ex) { throw new IllegalArgumentException("Không thể ghi metadata kiểm toán.", ex); }
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null || correlationId.isBlank()) correlationId = UUID.randomUUID().toString();
        String eventHash = hashEvent(id, now, actor == null ? null : actor.id(), actor == null ? "SYSTEM" : actor.role(),
                action, objectType, objectId, outcome, correlationId, safeMetadata, previousHash);
        jdbc.update("""
                INSERT INTO audit_events(id, occurred_at, actor_id, actor_role, action, object_type, object_id,
                  outcome, correlation_id, metadata, previous_hash, event_hash)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, now, actor == null ? null : actor.id(), actor == null ? "SYSTEM" : actor.role(),
                action, objectType, objectId, outcome, correlationId, safeMetadata, previousHash, eventHash);
        return new AuditEvent(id, now, actor == null ? null : actor.id(), actor == null ? "SYSTEM" : actor.role(),
                action, objectType, objectId, outcome, correlationId, safeMetadata, previousHash, eventHash);
    }

    public List<AuditEvent> forObject(Actor actor, String objectType, String objectId) {
        if (!actor.hasRole("EXAMINATION") && !actor.hasRole("ADMIN")) {
            String scopeQuery = actor.hasRole("DEPT_HEAD")
                    ? "SELECT COUNT(*) FROM transcripts x JOIN course_classes c ON c.id=x.course_class_id WHERE x.id=? AND c.department_id=?"
                    : "SELECT COUNT(*) FROM transcripts WHERE id=? AND lecturer_id=?";
            int allowed = jdbc.queryForObject(scopeQuery, Integer.class, objectId, actor.hasRole("DEPT_HEAD") ? actor.departmentId() : actor.id());
            if (allowed == 0) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền xem nhật ký này.");
        }
        return jdbc.query("SELECT * FROM audit_events WHERE object_type=? AND object_id=? ORDER BY event_seq",
                (rs, row) -> new AuditEvent(rs.getString("id"), rs.getObject("occurred_at", OffsetDateTime.class),
                        rs.getString("actor_id"), rs.getString("actor_role"), rs.getString("action"),
                        rs.getString("object_type"), rs.getString("object_id"), rs.getString("outcome"),
                        rs.getString("correlation_id"), rs.getString("metadata"), rs.getString("previous_hash"),
                        rs.getString("event_hash")), objectType, objectId);
    }

    public List<AuditEvent> recent(Actor actor, int limit) {
        if (!actor.hasRole("EXAMINATION") && !actor.hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Khảo thí được tra cứu toàn bộ nhật ký.");
        }
        return jdbc.query("SELECT * FROM audit_events ORDER BY event_seq DESC LIMIT ?",
                (rs, row) -> new AuditEvent(rs.getString("id"), rs.getObject("occurred_at", OffsetDateTime.class),
                        rs.getString("actor_id"), rs.getString("actor_role"), rs.getString("action"),
                        rs.getString("object_type"), rs.getString("object_id"), rs.getString("outcome"),
                        rs.getString("correlation_id"), rs.getString("metadata"), rs.getString("previous_hash"),
                        rs.getString("event_hash")), Math.max(1, Math.min(limit, 250)));
    }

    public AuditIntegrityResult verifyChain(Actor actor) {
        if (!actor.hasRole("EXAMINATION") && !actor.hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Khảo thí được kiểm tra toàn bộ chuỗi audit.");
        }
        List<AuditEvent> events = jdbc.query("SELECT * FROM audit_events ORDER BY event_seq",
                (rs, row) -> new AuditEvent(rs.getString("id"), rs.getObject("occurred_at", OffsetDateTime.class),
                        rs.getString("actor_id"), rs.getString("actor_role"), rs.getString("action"),
                        rs.getString("object_type"), rs.getString("object_id"), rs.getString("outcome"),
                        rs.getString("correlation_id"), rs.getString("metadata"), rs.getString("previous_hash"),
                        rs.getString("event_hash")));
        String previous = null;
        for (AuditEvent event : events) {
            String expected = hashEvent(event.id(), event.occurredAt(), event.actorId(), event.actorRole(), event.action(),
                    event.objectType(), event.objectId(), event.outcome(), event.correlationId(), event.metadata(), previous);
            if (!java.util.Objects.equals(previous, event.previousHash()) || !expected.equals(event.eventHash())) {
                return new AuditIntegrityResult(false, event.id(), events.size());
            }
            previous = event.eventHash();
        }
        return new AuditIntegrityResult(true, null, events.size());
    }

    private static String hashEvent(String id, OffsetDateTime time, String actorId, String actorRole, String action,
                                    String objectType, String objectId, String outcome, String correlationId,
                                    String metadata, String previousHash) {
        String canonical = String.join("|", id, time.toString(), actorId == null ? "system" : actorId,
                actorRole, action, objectType, objectId, outcome, correlationId, metadata,
                previousHash == null ? "" : previousHash);
        return sha256(canonical);
    }

    private static String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception ex) { throw new IllegalStateException("SHA-256 unavailable", ex); }
    }

    public record AuditIntegrityResult(boolean valid, String firstInvalidEventId, int checkedEvents) {}
}
