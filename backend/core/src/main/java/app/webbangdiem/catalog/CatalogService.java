package app.webbangdiem.catalog;

import app.webbangdiem.identity.Actor;
import app.webbangdiem.audit.AuditService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class CatalogService {
    private final JdbcTemplate jdbc;
    private final AuditService audit;

    public CatalogService(JdbcTemplate jdbc, AuditService audit) { this.jdbc = jdbc; this.audit = audit; }

    public List<CourseClass> classes(Actor actor, String academicYear, String semester, String query) {
        StringBuilder sql = new StringBuilder("""
                SELECT c.id, c.course_class_code, c.subject_name, d.code AS department_code,
                       d.name AS department_name, t.academic_year, t.semester, c.deadline_at,
                       c.locked, u.display_name AS lecturer_name
                FROM course_classes c
                JOIN departments d ON d.id = c.department_id
                JOIN academic_terms t ON t.id = c.academic_term_id
                JOIN app_users u ON u.id = c.lecturer_id
                WHERE 1 = 1
                """);
        var args = new java.util.ArrayList<Object>();
        if (actor.hasRole("LECTURER")) {
            sql.append(" AND c.lecturer_id = ?");
            args.add(actor.id());
        } else if (actor.hasRole("DEPT_HEAD")) {
            sql.append(" AND c.department_id = ?");
            args.add(actor.departmentId());
        } else if (!actor.hasRole("EXAMINATION") && !actor.hasRole("ADMIN")) {
            return List.of();
        }
        if (academicYear != null && !academicYear.isBlank()) { sql.append(" AND t.academic_year = ?"); args.add(academicYear); }
        if (semester != null && !semester.isBlank()) { sql.append(" AND t.semester = ?"); args.add(semester); }
        if (query != null && !query.isBlank()) {
            sql.append(" AND (LOWER(c.course_class_code) LIKE ? OR LOWER(c.subject_name) LIKE ? OR LOWER(u.display_name) LIKE ?)");
            String pattern = "%" + query.strip().toLowerCase() + "%";
            args.add(pattern); args.add(pattern); args.add(pattern);
        }
        sql.append(" ORDER BY t.academic_year DESC, c.course_class_code");
        return jdbc.query(sql.toString(), this::mapCourseClass, args.toArray());
    }

    public boolean canAccess(Actor actor, String courseClassId) {
        if (actor.hasRole("EXAMINATION") || actor.hasRole("ADMIN")) return true;
        String sql = actor.hasRole("LECTURER")
                ? "SELECT COUNT(*) FROM course_classes WHERE id = ? AND lecturer_id = ?"
                : "SELECT COUNT(*) FROM course_classes WHERE id = ? AND department_id = ?";
        return jdbc.queryForObject(sql, Integer.class, courseClassId, actor.hasRole("LECTURER") ? actor.id() : actor.departmentId()) > 0;
    }

    public CourseClass get(String courseClassId) {
        return jdbc.queryForObject("""
                SELECT c.id, c.course_class_code, c.subject_name, d.code AS department_code,
                       d.name AS department_name, t.academic_year, t.semester, c.deadline_at,
                       c.locked, u.display_name AS lecturer_name
                FROM course_classes c JOIN departments d ON d.id = c.department_id
                JOIN academic_terms t ON t.id = c.academic_term_id JOIN app_users u ON u.id = c.lecturer_id
                WHERE c.id = ?
                """, this::mapCourseClass, courseClassId);
    }

    public boolean isDeadlineOpen(CourseClass courseClass, Actor actor) {
        if (!courseClass.deadlineAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
            int locked = jdbc.update("UPDATE course_classes SET locked=TRUE WHERE id=? AND locked=FALSE", courseClass.id());
            if (locked > 0) audit.record(actor, "SUBMISSION_WINDOW_LOCKED", "course_class", courseClass.id(), "SUCCESS",
                    java.util.Map.of("deadlineAt", courseClass.deadlineAt().toString()));
            return false;
        }
        return !courseClass.locked();
    }

    public void createClass(String code, String subject, String lecturerId, String departmentId,
                            String termId, OffsetDateTime deadline, Actor actor) {
        String id = java.util.UUID.randomUUID().toString();
        jdbc.update("INSERT INTO course_classes(id, course_class_code, subject_name, lecturer_id, department_id, academic_term_id, deadline_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                id, code.strip(), subject.strip(), lecturerId, departmentId, termId, deadline);
        audit.record(actor, "CATALOG_CLASS_CREATED", "course_class", id, "SUCCESS", java.util.Map.of("courseClassCode", code.strip()));
    }

    public void updateDeadline(String courseClassId, OffsetDateTime deadline, Actor actor) {
        int transcripts = jdbc.queryForObject("SELECT COUNT(*) FROM transcripts WHERE course_class_id=?", Integer.class, courseClassId);
        if (transcripts > 0) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT, "Hồ sơ đã được nộp; không thể đổi ý nghĩa hạn của lớp đã tham chiếu.");
        int changed = jdbc.update("UPDATE course_classes SET deadline_at=?, locked=FALSE WHERE id=?", deadline, courseClassId);
        if (changed == 0) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Không tìm thấy lớp học phần.");
        audit.record(actor, "CATALOG_DEADLINE_CHANGED", "course_class", courseClassId, "SUCCESS", java.util.Map.of("deadlineAt", deadline.toString()));
    }

    public void setLocked(String courseClassId, boolean locked, Actor actor) {
        int changed = jdbc.update("UPDATE course_classes SET locked=? WHERE id=?", locked, courseClassId);
        if (changed == 0) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Không tìm thấy lớp học phần.");
        audit.record(actor, locked ? "COURSE_CLASS_LOCKED" : "COURSE_CLASS_UNLOCKED", "course_class", courseClassId,
                "SUCCESS", java.util.Map.of());
    }

    private CourseClass mapCourseClass(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        OffsetDateTime deadline = rs.getObject("deadline_at", OffsetDateTime.class);
        boolean locked = rs.getBoolean("locked") || !deadline.isAfter(OffsetDateTime.now(ZoneOffset.UTC));
        return new CourseClass(rs.getString("id"), rs.getString("course_class_code"), rs.getString("subject_name"),
                rs.getString("department_code"), rs.getString("department_name"), rs.getString("academic_year"),
                rs.getString("semester"), deadline, locked, rs.getString("lecturer_name"));
    }
}
