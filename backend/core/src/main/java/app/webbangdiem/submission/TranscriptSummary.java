package app.webbangdiem.submission;

import java.time.OffsetDateTime;

public record TranscriptSummary(String id, String courseClassId, String courseClassCode, String subjectName,
                                String lecturerName, String departmentName, String academicYear, String semester,
                                String fileName, String sha256, long fileSize, String status,
                                int revision, OffsetDateTime createdAt, String rejectionReason, int signatureCount) {}
