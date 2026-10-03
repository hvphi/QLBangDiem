package app.webbangdiem.catalog;

import app.webbangdiem.identity.DemoIdentity;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
@Profile("local")
public class DemoDataSeeder implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public DemoDataSeeder(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void run(ApplicationArguments args) {
        jdbc.update("INSERT INTO departments(id, code, name) SELECT ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM departments WHERE id = ?)",
                DemoIdentity.DEPARTMENT_ID, "CNTT", "Khoa Công nghệ thông tin", DemoIdentity.DEPARTMENT_ID);
        insertUser(DemoIdentity.LECTURER_ID, "Nguyễn Minh An", "an.nguyen@stage.local", "LECTURER");
        insertUser(DemoIdentity.DEPARTMENT_HEAD_ID, "Trần Thu Hà", "ha.tran@stage.local", "DEPT_HEAD");
        insertUser(DemoIdentity.EXAMINATION_ID, "Lê Vân", "van.le@stage.local", "EXAMINATION");
        insertUser(DemoIdentity.ADMIN_ID, "Quản trị staging", "admin@stage.local", "ADMIN");
        jdbc.update("INSERT INTO academic_terms(id, academic_year, semester, starts_on, ends_on) SELECT ?, ?, ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM academic_terms WHERE id = ?)",
                DemoIdentity.TERM_ID, "2026-2027", "HK1", LocalDate.now().minusDays(20), LocalDate.now().plusMonths(5), DemoIdentity.TERM_ID);
        insertClass("40000000-0000-0000-0000-000000000001", "CTDL-01", "Cấu trúc dữ liệu và giải thuật");
        insertClass("40000000-0000-0000-0000-000000000002", "WEB-02", "Phát triển ứng dụng web");
    }

    private void insertUser(String id, String name, String email, String role) {
        jdbc.update("INSERT INTO app_users(id, display_name, email, role, department_id) SELECT ?, ?, ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM app_users WHERE id = ?)",
                id, name, email, role, role.equals("EXAMINATION") ? null : DemoIdentity.DEPARTMENT_ID, id);
    }

    private void insertClass(String id, String code, String subject) {
        jdbc.update("INSERT INTO course_classes(id, course_class_code, subject_name, lecturer_id, department_id, academic_term_id, deadline_at) SELECT ?, ?, ?, ?, ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM course_classes WHERE id = ?)",
                id, code, subject, DemoIdentity.LECTURER_ID, DemoIdentity.DEPARTMENT_ID, DemoIdentity.TERM_ID,
                OffsetDateTime.now(ZoneOffset.UTC).plusDays(21), id);
    }
}
