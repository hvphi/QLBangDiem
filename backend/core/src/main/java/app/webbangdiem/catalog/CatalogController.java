package app.webbangdiem.catalog;

import app.webbangdiem.identity.ActorContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {
    private final CatalogService catalog;
    public CatalogController(CatalogService catalog) { this.catalog = catalog; }

    @GetMapping("/classes")
    public List<CourseClass> classes(@RequestParam(required = false) String academicYear,
                                     @RequestParam(required = false) String semester,
                                     @RequestParam(required = false) String q) {
        return catalog.classes(ActorContext.current(), academicYear, semester, q);
    }

    @PostMapping("/classes")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody CreateClassRequest request) {
        var actor = ActorContext.requireRole("ADMIN");
        catalog.createClass(request.courseClassCode(), request.subjectName(), request.lecturerId(),
                request.departmentId(), request.academicTermId(), request.deadlineAt(), actor);
    }

    @PatchMapping("/classes/{id}/deadline")
    public void updateDeadline(@PathVariable String id, @Valid @RequestBody DeadlineRequest request) {
        catalog.updateDeadline(id, request.deadlineAt(), ActorContext.requireRole("ADMIN"));
    }

    @PatchMapping("/classes/{id}/lock")
    public void setLocked(@PathVariable String id, @Valid @RequestBody LockRequest request) {
        catalog.setLocked(id, request.locked(), ActorContext.requireRole("ADMIN"));
    }

    public record CreateClassRequest(@NotBlank String courseClassCode, @NotBlank String subjectName,
                                     @NotBlank String lecturerId, @NotBlank String departmentId,
                                     @NotBlank String academicTermId, @NotNull java.time.OffsetDateTime deadlineAt) {}
    public record DeadlineRequest(@NotNull java.time.OffsetDateTime deadlineAt) {}
    public record LockRequest(@NotNull Boolean locked) {}
}
