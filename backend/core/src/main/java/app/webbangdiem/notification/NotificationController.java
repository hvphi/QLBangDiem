package app.webbangdiem.notification;

import app.webbangdiem.identity.ActorContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notifications;
    public NotificationController(NotificationService notifications) { this.notifications = notifications; }

    @GetMapping
    public List<NotificationService.NotificationItem> list() { return notifications.list(ActorContext.current()); }

    @PatchMapping("/{id}/read")
    public void markRead(@PathVariable String id) { notifications.markRead(ActorContext.current(), id); }
}
