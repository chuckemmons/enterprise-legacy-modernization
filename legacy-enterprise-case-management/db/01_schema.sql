-- PostgreSQL 12+; execute as the database owner before starting Tomcat.
CREATE TABLE IF NOT EXISTS app_users (
 id BIGSERIAL PRIMARY KEY, username VARCHAR(60) UNIQUE NOT NULL,
 password_hash VARCHAR(200) NOT NULL, display_name VARCHAR(120) NOT NULL,
 email VARCHAR(254), role VARCHAR(24) NOT NULL, department VARCHAR(80) NOT NULL,
 enabled BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE TABLE IF NOT EXISTS service_cases (
 id BIGSERIAL PRIMARY KEY, case_number VARCHAR(40) UNIQUE NOT NULL,
 title VARCHAR(180) NOT NULL, description TEXT NOT NULL,
 category VARCHAR(60) NOT NULL, priority VARCHAR(12) NOT NULL,
 status VARCHAR(32) NOT NULL, created_by VARCHAR(60) NOT NULL,
 assigned_to VARCHAR(60), department VARCHAR(80) NOT NULL,
 due_at TIMESTAMP, closed_at TIMESTAMP,
 created_at TIMESTAMP NOT NULL DEFAULT now(), updated_at TIMESTAMP NOT NULL DEFAULT now(),
 version INTEGER NOT NULL DEFAULT 0, archived BOOLEAN NOT NULL DEFAULT FALSE,
 overdue_notice_sent BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS ix_cases_work ON service_cases(status, assigned_to, archived);
CREATE INDEX IF NOT EXISTS ix_cases_created ON service_cases(created_at DESC);
CREATE TABLE IF NOT EXISTS case_notes (
 id BIGSERIAL PRIMARY KEY, case_id BIGINT NOT NULL REFERENCES service_cases(id),
 author VARCHAR(60) NOT NULL, body VARCHAR(4000) NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_notes_case ON case_notes(case_id, created_at);
CREATE TABLE IF NOT EXISTS case_documents (
 id BIGSERIAL PRIMARY KEY, case_id BIGINT NOT NULL REFERENCES service_cases(id),
 original_name VARCHAR(250) NOT NULL, content_type VARCHAR(120) NOT NULL,
 content_size INTEGER NOT NULL, payload BYTEA NOT NULL,
 uploaded_by VARCHAR(60) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_docs_case ON case_documents(case_id);
CREATE TABLE IF NOT EXISTS audit_events (
 id BIGSERIAL PRIMARY KEY, entity_type VARCHAR(40) NOT NULL, entity_id BIGINT,
 action_name VARCHAR(40) NOT NULL, actor VARCHAR(60) NOT NULL,
 details VARCHAR(4000) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_audit_time ON audit_events(created_at DESC);
CREATE TABLE IF NOT EXISTS case_notices (
 id BIGSERIAL PRIMARY KEY, recipient VARCHAR(60) NOT NULL,
 subject VARCHAR(180) NOT NULL, message VARCHAR(4000) NOT NULL,
 case_id BIGINT, is_read BOOLEAN NOT NULL DEFAULT FALSE,
 sent_at TIMESTAMP, created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_notices_recipient ON case_notices(recipient, is_read, created_at DESC);
