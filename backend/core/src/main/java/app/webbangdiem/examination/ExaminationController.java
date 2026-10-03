package app.webbangdiem.examination;

import app.webbangdiem.identity.ActorContext;
import app.webbangdiem.submission.SubmissionController;
import app.webbangdiem.submission.TranscriptSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/examination")
public class ExaminationController {
    private final JdbcTemplate jdbc;
    private final SubmissionController submissions;

    public ExaminationController(JdbcTemplate jdbc, SubmissionController submissions) {
        this.jdbc = jdbc; this.submissions = submissions;
    }

    @GetMapping("/transcripts")
    public TranscriptPage transcripts(@RequestParam(required = false) String academicYear,
                                      @RequestParam(required = false) String semester,
                                      @RequestParam(required = false) String department,
                                      @RequestParam(required = false) String status,
                                      @RequestParam(required = false) String q,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        ActorContext.requireRole("EXAMINATION", "ADMIN");
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        var args = new java.util.ArrayList<Object>();
        if (academicYear != null && !academicYear.isBlank()) { where.append(" AND t.academic_year=?"); args.add(academicYear); }
        if (semester != null && !semester.isBlank()) { where.append(" AND t.semester=?"); args.add(semester); }
        if (department != null && !department.isBlank()) { where.append(" AND d.code=?"); args.add(department); }
        if (status != null && !status.isBlank()) { where.append(" AND x.status=?"); args.add(status); }
        if (q != null && !q.isBlank()) {
            where.append(" AND (LOWER(c.course_class_code) LIKE ? OR LOWER(c.subject_name) LIKE ? OR LOWER(l.display_name) LIKE ?)");
            String pattern = "%" + q.strip().toLowerCase() + "%";
            args.add(pattern); args.add(pattern); args.add(pattern);
        }
        String from = " FROM transcripts x JOIN course_classes c ON c.id=x.course_class_id JOIN app_users l ON l.id=x.lecturer_id JOIN departments d ON d.id=c.department_id JOIN academic_terms t ON t.id=c.academic_term_id";
        long total = jdbc.queryForObject("SELECT COUNT(*)" + from + where, Long.class, args.toArray());
        var pageArgs = new java.util.ArrayList<>(args);
        pageArgs.add(safeSize); pageArgs.add((long) safePage * safeSize);
        List<String> ids = jdbc.query("SELECT x.id" + from + where + " ORDER BY x.created_at DESC LIMIT ? OFFSET ?",
                (rs, row) -> rs.getString(1), pageArgs.toArray());
        List<TranscriptSummary> items = ids.stream().map(submissions::summary).toList();
        return new TranscriptPage(items, safePage, safeSize, total);
    }
}
