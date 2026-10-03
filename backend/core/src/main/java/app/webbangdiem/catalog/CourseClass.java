package app.webbangdiem.catalog;

import java.time.OffsetDateTime;

public record CourseClass(
        String id,
        String courseClassCode,
        String subjectName,
        String departmentCode,
        String departmentName,
        String academicYear,
        String semester,
        OffsetDateTime deadlineAt,
        boolean locked,
        String lecturerName
) {}
