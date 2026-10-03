package app.webbangdiem.audit;

import java.time.OffsetDateTime;

public record AuditEvent(String id, OffsetDateTime occurredAt, String actorId, String actorRole,
                         String action, String objectType, String objectId, String outcome,
                         String correlationId, String metadata, String previousHash, String eventHash) {}
