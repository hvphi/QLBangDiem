package app.webbangdiem.audit;

import app.webbangdiem.identity.ActorContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {
    private final AuditService audit;
    public AuditController(AuditService audit) { this.audit = audit; }

    @GetMapping("/events")
    public List<AuditEvent> recent(@RequestParam(defaultValue = "100") int limit) {
        return audit.recent(ActorContext.current(), limit);
    }

    @GetMapping("/integrity")
    public AuditService.AuditIntegrityResult verifyIntegrity() {
        return audit.verifyChain(ActorContext.current());
    }

    @GetMapping("/transcripts/{id}")
    public List<AuditEvent> transcript(@PathVariable String id) {
        return audit.forObject(ActorContext.current(), "transcript", id);
    }
}
