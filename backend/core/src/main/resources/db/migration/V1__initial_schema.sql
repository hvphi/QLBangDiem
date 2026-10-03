CREATE TABLE departments (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(180) NOT NULL
);

CREATE TABLE app_users (
    id VARCHAR(36) PRIMARY KEY,
    display_name VARCHAR(160) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    role VARCHAR(32) NOT NULL CHECK (role IN ('LECTURER', 'DEPT_HEAD', 'EXAMINATION', 'ADMIN')),
    department_id VARCHAR(36) REFERENCES departments(id),
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE academic_terms (
    id VARCHAR(36) PRIMARY KEY,
    academic_year VARCHAR(20) NOT NULL,
    semester VARCHAR(12) NOT NULL,
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    UNIQUE (academic_year, semester),
    CHECK (starts_on <= ends_on)
);

CREATE TABLE course_classes (
    id VARCHAR(36) PRIMARY KEY,
    course_class_code VARCHAR(50) NOT NULL,
    subject_name VARCHAR(255) NOT NULL,
    lecturer_id VARCHAR(36) NOT NULL REFERENCES app_users(id),
    department_id VARCHAR(36) NOT NULL REFERENCES departments(id),
    academic_term_id VARCHAR(36) NOT NULL REFERENCES academic_terms(id),
    deadline_at TIMESTAMP WITH TIME ZONE NOT NULL,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (course_class_code, academic_term_id)
);
CREATE INDEX ix_course_classes_lecturer ON course_classes(lecturer_id, academic_term_id);
CREATE INDEX ix_course_classes_department ON course_classes(department_id, academic_term_id);

CREATE TABLE transcripts (
    id VARCHAR(36) PRIMARY KEY,
    course_class_id VARCHAR(36) NOT NULL REFERENCES course_classes(id),
    lecturer_id VARCHAR(36) NOT NULL REFERENCES app_users(id),
    revision INTEGER NOT NULL CHECK (revision > 0),
    file_name VARCHAR(255) NOT NULL,
    object_key VARCHAR(1024) NOT NULL UNIQUE,
    file_hash_sha256 CHAR(64) NOT NULL,
    file_size BIGINT NOT NULL CHECK (file_size > 0),
    status VARCHAR(32) NOT NULL CHECK (status IN ('LECTURER_SIGNED', 'DEPT_APPROVED', 'ARCHIVED', 'REJECTED')),
    idempotency_key VARCHAR(160),
    supersedes_transcript_id VARCHAR(36) REFERENCES transcripts(id),
    rejection_reason VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (course_class_id, revision)
);
CREATE INDEX ix_transcripts_status_created ON transcripts(status, created_at DESC);
CREATE INDEX ix_transcripts_course_class ON transcripts(course_class_id, revision DESC);
CREATE UNIQUE INDEX uq_transcripts_lecturer_idempotency ON transcripts(lecturer_id, idempotency_key);

CREATE TABLE digital_signatures (
    id VARCHAR(36) PRIMARY KEY,
    transcript_id VARCHAR(36) NOT NULL REFERENCES transcripts(id),
    signer_type VARCHAR(24) NOT NULL CHECK (signer_type IN ('LECTURER', 'DEPT_HEAD')),
    signer_name VARCHAR(160),
    certificate_serial VARCHAR(200),
    issuer_dn VARCHAR(1000),
    signed_at TIMESTAMP WITH TIME ZONE,
    timestamp_tsa_token TEXT,
    validation_status VARCHAR(24) NOT NULL CHECK (validation_status IN ('VALID', 'INVALID', 'REVOKED', 'EXPIRED', 'UNKNOWN', 'UNAVAILABLE')),
    validation_reason VARCHAR(1000),
    validated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_signatures_transcript ON digital_signatures(transcript_id, signer_type);

CREATE TABLE audit_events (
    id VARCHAR(36) PRIMARY KEY,
    event_seq BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL UNIQUE,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    actor_id VARCHAR(36),
    actor_role VARCHAR(32) NOT NULL,
    action VARCHAR(80) NOT NULL,
    object_type VARCHAR(80) NOT NULL,
    object_id VARCHAR(100) NOT NULL,
    outcome VARCHAR(24) NOT NULL,
    correlation_id VARCHAR(100),
    metadata TEXT NOT NULL,
    previous_hash CHAR(64),
    event_hash CHAR(64) NOT NULL UNIQUE
);
CREATE INDEX ix_audit_object_time ON audit_events(object_type, object_id, occurred_at DESC);
CREATE INDEX ix_audit_actor_time ON audit_events(actor_id, occurred_at DESC);

CREATE TABLE notifications (
    id VARCHAR(36) PRIMARY KEY,
    recipient_id VARCHAR(36) NOT NULL REFERENCES app_users(id),
    transcript_id VARCHAR(36) REFERENCES transcripts(id),
    kind VARCHAR(40) NOT NULL,
    message VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    read_at TIMESTAMP WITH TIME ZONE,
    UNIQUE (recipient_id, transcript_id, kind)
);
CREATE INDEX ix_notifications_recipient_time ON notifications(recipient_id, created_at DESC);
