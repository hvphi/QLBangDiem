package app.webbangdiem.notification;

import app.webbangdiem.identity.Actor;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {
    private final JdbcTemplate jdbc;
    public NotificationService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @EventListener
    public void handle(WorkflowNotificationEvent event) {
        jdbc.update("""
                INSERT INTO notifications(id, recipient_id, transcript_id, kind, message, created_at)
                SELECT ?, ?, ?, ?, ?, ? WHERE NOT EXISTS (
                  SELECT 1 FROM notifications WHERE recipient_id=? AND transcript_id=? AND kind=?
                )
                """, UUID.randomUUID().toString(), event.recipientId(), event.transcriptId(), event.kind(),
                event.message(), OffsetDateTime.now(ZoneOffset.UTC), event.recipientId(), event.transcriptId(), event.kind());
    }

    public List<NotificationItem> list(Actor actor) {
        return jdbc.query("SELECT id, transcript_id, kind, message, created_at, read_at FROM notifications WHERE recipient_id=? ORDER BY created_at DESC LIMIT 100",
                (rs, row) -> new NotificationItem(rs.getString("id"), rs.getString("transcript_id"), rs.getString("kind"),
                        rs.getString("message"), rs.getObject("created_at", OffsetDateTime.class), rs.getObject("read_at", OffsetDateTime.class)), actor.id());
    }

    public void markRead(Actor actor, String id) {
        int changed = jdbc.update("UPDATE notifications SET read_at=? WHERE id=? AND recipient_id=? AND read_at IS NULL",
                OffsetDateTime.now(ZoneOffset.UTC), id, actor.id());
        if (changed == 0 && jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE id=? AND recipient_id=?", Integer.class, id, actor.id()) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo.");
        }
    }

    public record NotificationItem(String id, String transcriptId, String kind, String message,
                                   OffsetDateTime createdAt, OffsetDateTime readAt) {}
}
